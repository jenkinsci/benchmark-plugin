/**
 * MIT license
 * Copyright 2017 Autodesk, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions
 * of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package org.jenkinsci.plugins.benchmark.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.benchmark.exceptions.ValidationException;
import org.jenkinsci.plugins.benchmark.parsers.FormatSelector;
import org.jenkinsci.plugins.benchmark.parsers.MapperBase;
import org.jenkinsci.plugins.benchmark.parsers.jUnitJenkins;
import org.jenkinsci.plugins.benchmark.schemas.Schema;
import org.jenkinsci.plugins.benchmark.thresholds.Threshold;
import org.jenkinsci.plugins.benchmark.utilities.ResetMemoryTask;
import org.jenkinsci.plugins.benchmark.utilities.RunnableJenkinsReader;
import org.jenkinsci.plugins.benchmark.utilities.RunnableReader;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.jenkinsci.plugins.benchmark.thresholds.ThresholdDescriptor;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.apache.commons.io.FileUtils.readFileToString;

/**
 * Benchmark post-build step/publisher
 *
 * <p>
 * Core of the plugin, from which all actions either display of backend logic derive.
 * <p>
 *
 * @author Daniel Mercier
 * @since 5/16/2017.
 */
@ExportedBean
public class BenchmarkPublisher extends Recorder {

    private static final  Map<String, Schema>   schemaResources;
    static{
        // Registered resources
        schemaResources = new HashMap<String, Schema>();
        schemaResources.put("defaultSchema",  new Schema(Messages.BenchmarkPublisher_Default(),  "schemas/default",  Messages.BenchmarkPublisher_DefaultSchemaDescription(), Schema.Json_format | Schema.Xml_format ));
        schemaResources.put("simplestSchema", new Schema(Messages.BenchmarkPublisher_Simplest(), "schemas/simplest", Messages.BenchmarkPublisher_SimplestSchemaDescription(), Schema.Json_format | Schema.Xml_format ));
    }

    // Variables

    public static final int                 TIME_DELAY_MS = 120000;

    private static final Logger log = Logger.getLogger(BenchmarkPublisher.class.getName());

    private final String                    inputLocation;
    private final String                    schemaSelection;
    private final Boolean                   truncateStrings;
    private final String                    altInputSchema;
    private final String                    altInputSchemaLocation;

    // Information from the threshold fields
    private List<? extends Threshold>       altThresholds;

    private transient MapperBase            map;
    private transient Timer                 timer;
    private transient Integer               selectedResult;
    private transient Integer               selectedBuild;
    private transient AbstractProject<?, ?> project;

    // Constructor

    @DataBoundConstructor
    public BenchmarkPublisher(String inputLocation, String schemaSelection, Boolean truncateStrings, String altInputSchema, String altInputSchemaLocation) {
        this.inputLocation = inputLocation;
        this.schemaSelection = schemaSelection;
        this.truncateStrings = truncateStrings;
        this.altInputSchema = altInputSchema;
        this.altInputSchemaLocation = altInputSchemaLocation;
        this.altThresholds = new ArrayList<Threshold>();
    }

    // Functions

    @DataBoundSetter
    public void setThresholds(List<? extends Threshold> thresholds) { this.altThresholds = thresholds; }

    @Override // Function to enforce order of build steps
    public BuildStepMonitor getRequiredMonitorService() { return BuildStepMonitor.NONE; }

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        this.project = project;
        return new BenchmarkProjectAction(project, this);
    }

    @Override
    public Collection<? extends Action> getProjectActions(AbstractProject<?,?> project){
        List<Action> actions = new ArrayList<Action>();
        actions.add(getProjectAction(project));
        actions.add(new BenchmarkResultAction(project, this));
        return actions;
    }

    @Override // This is where the 'build' step is executed. To access descriptor variables, use: getDescriptor()
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {

        boolean failed = false;

        listener.getLogger().println(Messages.BenchmarkPublisher_CollectionOfResultsStarted());
        try {

            Integer buildNumber = build.getNumber();
            String projectName = build.getProject().getName();

            // First testing for any specified location
            // If not, check for existing Jenkins Test Report inside the previous builds
            if (inputLocation == null || inputLocation.isEmpty()){

                MapperBase mapper = getRawResults(build);
                if (mapper != null) {
                    // Update file with condensed results
                    String oFilename = build.getProject().getRootDir().getAbsolutePath() + File.separator + "BenchmarkCondensed.json";
                    mapper.exportCondensedToFile(oFilename, projectName, buildNumber);
                }

            } else {

                // Load the schema
                String schemaText;
                if (schemaSelection.contains("customSchema")) {
                    if (altInputSchema == null || altInputSchema.isEmpty()) {
                        if (altInputSchemaLocation == null || altInputSchemaLocation.isEmpty()) {
                            listener.getLogger().println(Messages.BenchmarkPublisher_CustomSchemaEmpty());
                            throw new IOException(Messages.BenchmarkPublisher_CustomSchemaEmpty());
                        } else {
                            File file = new File(altInputSchemaLocation);
                            FilePath filePath = new FilePath(file);
                            if (filePath.exists()) {
                                InputStream inputStream = filePath.read();
                                schemaText = IOUtils.toString(inputStream);
                            } else {
                                listener.getLogger().println(Messages.BenchmarkPublisher_CustomSchemaFileNotDetected());
                                throw new IOException(Messages.BenchmarkPublisher_CustomSchemaFileNotDetected());
                            }
                        }
                    } else {
                        schemaText = altInputSchema;
                    }
                } else {
                    Schema schema = schemaResources.get(schemaSelection);
                    if (schema == null) {
                        listener.getLogger().println(Messages.BenchmarkPublisher_SelectedSchemaDoesNotExist());
                        throw new IOException(Messages.BenchmarkPublisher_SelectedSchemaDoesNotExist());
                    } else {
                        String schemaAddress = schema.getLocation();
                        if (inputLocation.contains(".xml")) {
                            schemaAddress += ".xml";
                        } else {
                            schemaAddress += ".json";
                        }
                        ClassLoader classLoader = getClass().getClassLoader();
                        File file = new File(classLoader.getResource(schemaAddress).getFile());
                        schemaText = readFileToString(file);
                    }
                }

                // Map results
                FormatSelector selector = new FormatSelector(build, inputLocation, schemaText, truncateStrings, listener);
                MapperBase mapper = selector.getMapper();

                // Load additional Thresholds
                if (altThresholds.size() > 0) {
                    mapper.addAllThresholds(altThresholds);
                }

                MapperBase base = getRawResults(build);
                failed = mapper.checkThresholds(base);

                // Log mapper core information
                mapper.logKeyData(listener, altThresholds.size());

                // Export build file
                String outputFilename = build.getRootDir().getAbsolutePath() + File.separator + "BenchmarkResult.json";
                mapper.exportToFile(outputFilename, projectName, buildNumber);

                // Merge content
                mapper.mergeWith(base);

                // Update file with condensed results
                String oFilename = build.getProject().getRootDir().getAbsolutePath() + File.separator + "BenchmarkCondensed.json";
                mapper.exportCondensedToFile(oFilename, projectName, buildNumber);
            }

        } catch(Exception e) {
            listener.getLogger().println(e.getMessage());
            listener.getLogger().println(Messages.BenchmarkPublisher_ErrorDetectedDuringPostBuild());
            build.setResult(Result.FAILURE);
            return false;
        }
        if (failed) {
            listener.getLogger().println(Messages.BenchmarkPublisher_CollectionSuccessButValidationFailure());
            build.setResult(Result.FAILURE);
            return false;
        }
        listener.getLogger().println(Messages.BenchmarkPublisher_PluginSuccessfull());
        return true;
    }

    /**
     * Return whether result files are present
     * @param build Jenkins build instance
     * @return Whether raw results are available
     * @throws NullPointerException If null pointer detected
     * @throws FileNotFoundException If file not found
     * @throws JsonIOException If I/O errors occur
     * @throws JsonSyntaxException If JSON syntax invalid
     */
    public Boolean hasResults(AbstractBuild build) throws NullPointerException, FileNotFoundException, JsonIOException, JsonSyntaxException {
        String condensedFilename = build.getProject().getRootDir().getAbsolutePath() + File.separator + "BenchmarkCondensed.json";
        File oFile = new File(condensedFilename);
        if (oFile.exists()) {
            return true;
        }
        return false;
    }

    /**
     * Retrieved and assemble all the build results into the mapper construct
     * @param build Jenkins build instance
     * @return Class to raw results
     * @throws NullPointerException If null pointer detected
     * @throws InterruptedException Interrupted Exception
     * @throws ValidationException Validation exception
     * @throws IOException If file not found
     * @throws JsonIOException If I/O errors occur
     * @throws JsonSyntaxException If JSON syntax invalid
     */
    public MapperBase getRawResults(AbstractBuild build) throws NullPointerException, InterruptedException, ValidationException, IOException,  JsonIOException, JsonSyntaxException {

        if (inputLocation == null || inputLocation.isEmpty()){

            jUnitJenkins mapper = new jUnitJenkins(build.getNumber(), truncateStrings);

            // Load condensed file if present
            StringBuffer condensedFilename = new StringBuffer();
            condensedFilename.append(build.getProject().getRootDir().getAbsolutePath());
            condensedFilename.append(File.separator);
            condensedFilename.append("BenchmarkCondensed.json");
            mapper.importCondensedFromFile(condensedFilename.toString());

            // Load the files between the current build and the condensed one sequentially
            while (build != null && build.getNumber() != mapper.getBuild()){
                FilePath workspace = build.getWorkspace();
                if (!workspace.isDirectory()) {
                    log.warning(Messages.BenchmarkPublisher_WorkspaceIsNotDetected());
                    throw new IOException(Messages.BenchmarkPublisher_WorkspaceIsNotDetected());
                }

                StringBuffer rawFilename = new StringBuffer();
                rawFilename.append(build.getRootDir().getAbsolutePath());
                rawFilename.append(File.separator);
                rawFilename.append("junitResult.xml");
                mapper.importFromFile(build.getNumber(), rawFilename.toString());
                build = build.getPreviousBuild();
            }

            if (build == null) {
                return mapper;
            }

            // Load the files below the condensed one in parallel
            int cores = Runtime.getRuntime().availableProcessors() - 1;
            AbstractBuild firstBuild = project.getFirstBuild();
            int numberOfBuilds = build.getNumber() - firstBuild.getNumber();
            int buildsPerSegment = 4;
            if (numberOfBuilds > cores * 4) {
                buildsPerSegment = numberOfBuilds / cores;
            }

            ExecutorService server = Executors.newFixedThreadPool(cores);

            // Launch parallel threads
            AbstractBuild startBuild = build;
            AbstractBuild endBuild = startBuild;
            do {
                int i = 0;
                while (i < buildsPerSegment && endBuild != null) {
                    endBuild = endBuild.getPreviousBuild();
                    i++;
                }
                server.execute(new RunnableJenkinsReader(startBuild, endBuild, mapper));
                startBuild = endBuild;
            } while (startBuild != null);

            server.shutdown();

            // Blocks until all tasks have completed execution after a shutdown request
            server.awaitTermination(5, TimeUnit.MINUTES);

            return mapper;

        } else {

            MapperBase mapper = new MapperBase(build.getNumber(), truncateStrings);

            // Load condensed file if present
            StringBuffer condensedFilename = new StringBuffer(build.getProject().getRootDir().getAbsolutePath());
            condensedFilename.append(File.separator);
            condensedFilename.append("BenchmarkCondensed.json");
            if (!mapper.importCondensedFromFile(condensedFilename.toString())) {
                return null;
            }

            // Load the files between the current build and the condensed one sequentially
            while (build != null && build.getNumber() != mapper.getBuild()){
                build = build.getPreviousBuild();
            }

            if (build == null) {
                return mapper;
            }

            // Load the files below the condensed one in parallel
            int cores = Runtime.getRuntime().availableProcessors() - 1;
            AbstractBuild firstBuild = project.getFirstBuild();
            int numberOfBuilds = build.getNumber() - firstBuild.getNumber();
            int buildsPerSegment = 4;
            if (numberOfBuilds > cores * 4 ) {
                buildsPerSegment = numberOfBuilds / cores;
            }

            ExecutorService server = Executors.newFixedThreadPool(cores);

            // Launch parallel threads
            AbstractBuild startBuild = build;
            AbstractBuild endBuild = startBuild;
            do{
                int i = 0;
                while (i < buildsPerSegment && endBuild != null) {
                    endBuild = endBuild.getPreviousBuild();
                    i++;
                }
                server.execute(new RunnableReader(startBuild, endBuild, mapper));
                startBuild = endBuild;
            } while (startBuild != null);

            server.shutdown();

            // Blocks until all tasks have completed execution after a shutdown request
            server.awaitTermination(5, TimeUnit.MINUTES);

            return mapper;
        }
    }

    /**
     * fill All Results from files
     */
    public void fillAllResults(){
        try {
            resetClock();
            AbstractBuild build = project.getLastBuild();
            if (build != null) {
                MapperBase base = this.getMapper(build);
                base.setBuild(build.getNumber());
            }
        } catch (Exception e) {
            log.info(Messages.BenchmarkPublisher_ResultCollectionErrorDetected());
            log.info(Messages.BenchmarkPublisher_ResultCollectionErrorMessage(e.getMessage()));
        }
    }

    public void resetClock(){
        Timer timer = getTimer();
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        setTimer(timer);

        ResetMemoryTask task = new ResetMemoryTask(this);
        timer.schedule(task, BenchmarkPublisher.TIME_DELAY_MS);
    }

    public void resetMemory(){
        this.map = null;
        Timer timer = getTimer();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        System.gc();
    }

    // Setters

    public void setTimer(Timer timer) { this.timer = timer; }
    public void setSelectedResult(Integer selectedResult) { this.selectedResult = selectedResult; }
    public void setSelectedBuild(Integer selectedBuild) { this.selectedBuild = selectedBuild; }
    public void setMapper(MapperBase mapper){ this.map = mapper;}

    // Getters

    public String getInputLocation() { return inputLocation; }
    public String getSchemaSelection() { return schemaSelection; }
    public Boolean getTruncateStrings() { return truncateStrings; }
    public String getAltInputSchema() { return altInputSchema; }
    public List<? extends Threshold> getThresholds() { return altThresholds; }
    public Timer getTimer() { return timer; }
    public Integer getSelectedResult() { return selectedResult; }
    public Integer getSelectedBuild() { return selectedBuild; }
    public MapperBase getMapper(){ return map; }
    public MapperBase getMapper(AbstractBuild build) throws NullPointerException, InterruptedException, ValidationException, IOException,  JsonIOException, JsonSyntaxException {
        MapperBase base = this.map;
        if (base == null || build.getNumber() != base.getBuild()) {
            base = this.getRawResults(build);
            this.map = base;
        }
        return base;
    }

    /**
     * Descriptor for {@link BenchmarkPublisher}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     * This is the class in charge of all interactions by the UI components.
     *
     * See <tt>src/main/resources/org/jenkinsci/plugins/benchmark/BenchmarkPublisher/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */

        /** In order to load the persisted global configuration, you have to call load() in the constructor. */
        public DescriptorImpl () {
            load();
        }

        /** This human readable name is used in the configuration screen. */
        public String getDisplayName () {
            return Messages.BenchmarkPublisher_DisplayName();
        }

        public List<ThresholdDescriptor> getThresholdDescriptors() {return ThresholdDescriptor.all();}

        /** Indicates that this builder can be used with all kinds of project types */
        public boolean isApplicable (Class<? extends AbstractProject> aClass) { return true; }

        @Override /** To persist global configuration information, set that to properties and call save().*/
        public boolean configure (StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure (req, formData);
        }

        /**
         * Fill list of schema types
         * @return list of schema types
         */
        public ListBoxModel doFillSchemaSelectionItems() {

            ListBoxModel items = new ListBoxModel();
            for (Map.Entry<String, Schema> schema:schemaResources.entrySet()){
                items.add(schema.getValue().getDisplayName(), schema.getKey());
            }
            items.add(Messages.BenchmarkPublisher_Custom(), "customSchema");
            return items;
        }

        /**
         * Check Input Location
         * @param altInputSchema Custom schema as inserted by user
         * @return Validation result
         */
        public FormValidation doCheckAltInputSchema(@QueryParameter String altInputSchema) {
            if (altInputSchema == null || altInputSchema.isEmpty()) {
                return FormValidation.ok();
            }
            try {
                FormatSelector.checkFormat(altInputSchema);
                return FormValidation.ok();
            } catch(Exception e) {
                return FormValidation.error(Messages.BenchmarkPublisher_ContentDoesNotComplyWithFormat());
            }
        }
    }
}

