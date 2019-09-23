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

import java.io.*;
import java.nio.charset.StandardCharsets;
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
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.benchmark.exceptions.ValidationException;
import org.jenkinsci.plugins.benchmark.parsers.FormatSelector;
import org.jenkinsci.plugins.benchmark.parsers.MapperBase;
import org.jenkinsci.plugins.benchmark.parsers.JUnitJenkins;
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

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

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
public class BenchmarkPublisher extends Recorder implements SimpleBuildStep {



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

    private final String                      inputLocation;
    private final String                      schemaSelection;
    private final Boolean                     truncateStrings;
    private final String                      altInputSchema;
    private final String                      altInputSchemaLocation;

    // Information from the threshold fields
    private List<? extends Threshold>   altThresholds;

    private transient MapperBase map;
    private transient Timer      timer;
    private transient Integer    selectedResult;
    private transient Integer    selectedBuild;

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

    @Override // Function to enforce order of build steps
    public BuildStepMonitor getRequiredMonitorService() { return BuildStepMonitor.NONE; }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener) throws InterruptedException, IOException {
        Job project = run.getParent();

        run.addAction(new BenchmarkProjectAction(project, this));
        run.addAction(new BenchmarkResultAction(project, this));

        boolean failed = false;

        taskListener.getLogger().println(Messages.BenchmarkPublisher_CollectionOfResultsStarted());
        try {

            Integer buildNumber = run.getNumber();
            String projectName = run.getParent().getName();

            // First testing for any specified location
            // If not, check for existing Jenkins Test Report inside the previous builds
            if (inputLocation == null || inputLocation.isEmpty()){

                MapperBase mapper = getRawResults(run);
                if (mapper != null) {
                    // Update file with condensed results
                    String oFilename = run.getParent().getRootDir().getAbsolutePath() + File.separator + "BenchmarkCondensed.json";
                    mapper.exportCondensedToFile(oFilename, projectName, buildNumber);
                }

            } else {

                // Load the schema
                String schemaText;
                if (schemaSelection.contains("customSchema")) {
                    if (altInputSchema == null || altInputSchema.isEmpty()) {
                        if (altInputSchemaLocation == null || altInputSchemaLocation.isEmpty()) {
                            taskListener.getLogger().println(Messages.BenchmarkPublisher_CustomSchemaEmpty());
                            throw new IOException(Messages.BenchmarkPublisher_CustomSchemaEmpty());
                        } else {
                            File file = new File(altInputSchemaLocation);
                            FilePath newFilePath = new FilePath(file);
                            if (newFilePath.exists()) {
                                InputStream inputStream = newFilePath.read();
                                schemaText = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                                inputStream.close();
                            } else {
                                taskListener.getLogger().println(Messages.BenchmarkPublisher_CustomSchemaFileNotDetected());
                                throw new IOException(Messages.BenchmarkPublisher_CustomSchemaFileNotDetected());
                            }
                        }
                    } else {
                        schemaText = altInputSchema;
                    }
                } else {
                    Schema schema = schemaResources.get(schemaSelection);
                    if (schema == null) {
                        taskListener.getLogger().println(Messages.BenchmarkPublisher_SelectedSchemaDoesNotExist());
                        throw new IOException(Messages.BenchmarkPublisher_SelectedSchemaDoesNotExist());
                    } else {
                        String schemaAddress = schema.getLocation();
                        if (inputLocation.contains(".xml")) {
                            schemaAddress += ".xml";
                        } else {
                            schemaAddress += ".json";
                        }
                        ClassLoader classLoader = getClass().getClassLoader();
                        InputStream fileStream = classLoader.getResource(schemaAddress).openStream();
                        schemaText = IOUtils.toString(fileStream, StandardCharsets.UTF_8);
                        fileStream.close();
                    }
                }

                // Map results
                FormatSelector selector = new FormatSelector(run, filePath, inputLocation, schemaText, truncateStrings, taskListener);
                MapperBase mapper = selector.getMapper();

                // Load additional Thresholds
                if (altThresholds.size() > 0) {
                    mapper.addAllThresholds(altThresholds);
                }

                MapperBase base = getRawResults(run);
                failed = mapper.checkThresholds(base);

                // Log mapper core information
                mapper.logKeyData(taskListener, altThresholds.size());

                // Export build file
                String outputFilename = run.getRootDir().getAbsolutePath() + File.separator + "BenchmarkResult.json";
                mapper.exportToFile(outputFilename, projectName, buildNumber);

                // Merge content
                mapper.mergeWith(base);

                // Update file with condensed results
                String oFilename = run.getParent().getRootDir().getAbsolutePath() + File.separator + "BenchmarkCondensed.json";
                mapper.exportCondensedToFile(oFilename, projectName, buildNumber);
            }

        } catch(ValidationException e) {
            taskListener.getLogger().println(e.getMessage());
            taskListener.getLogger().println(Messages.BenchmarkPublisher_ErrorDetectedDuringPostBuild());
            run.setResult(Result.FAILURE);
            return;
        }
        if (failed) {
            taskListener.getLogger().println(Messages.BenchmarkPublisher_CollectionSuccessButValidationFailure());
            run.setResult(Result.UNSTABLE);
            return;
        }
        taskListener.getLogger().println(Messages.BenchmarkPublisher_PluginSuccessfull());
    }

    /**
     * Return whether result files are present
     * @param run Jenkins run instance
     * @return Whether raw results are available
     * @throws NullPointerException If null pointer detected
     * @throws FileNotFoundException If file not found
     * @throws JsonIOException If I/O errors occur
     * @throws JsonSyntaxException If JSON syntax invalid
     */
    public Boolean hasResults(Run run) throws NullPointerException, FileNotFoundException, JsonIOException, JsonSyntaxException {
        String condensedFilename = run.getParent().getRootDir().getAbsolutePath() + File.separator + "BenchmarkCondensed.json";
        File oFile = new File(condensedFilename);
        return oFile.exists();
    }

    /**
     * Retrieved and assemble all the build results into the mapper construct
     * @param run Jenkins run instance
     * @return Class to raw results
     * @throws NullPointerException If null pointer detected
     * @throws InterruptedException Interrupted Exception
     * @throws ValidationException Validation exception
     * @throws IOException If file not found
     * @throws JsonIOException If I/O errors occur
     * @throws JsonSyntaxException If JSON syntax invalid
     */
    public MapperBase getRawResults(Run<?, ?> run) throws NullPointerException, InterruptedException, ValidationException, IOException,  JsonIOException, JsonSyntaxException {
        Job project = run.getParent();

        if (inputLocation == null || inputLocation.isEmpty()){

            JUnitJenkins mapper = new JUnitJenkins(run.getNumber(), truncateStrings);

            // Load condensed file if present
            StringBuffer condensedFilename = new StringBuffer();
            condensedFilename.append(run.getParent().getRootDir().getAbsolutePath());
            condensedFilename.append(File.separator);
            condensedFilename.append("BenchmarkCondensed.json");
            mapper.importCondensedFromFile(condensedFilename.toString());

            // Load the files between the current build and the condensed one sequentially
            while (run != null && run.getNumber() != mapper.getBuild()){

                StringBuffer rawFilename = new StringBuffer();
                rawFilename.append(run.getRootDir().getAbsolutePath());
                rawFilename.append(File.separator);
                rawFilename.append("junitResult.xml");
                mapper.importFromFile(run.getNumber(), rawFilename.toString());
                run = run.getPreviousBuild();
            }

            if (run == null) {
                return mapper;
            }

            // Load the files below the condensed one in parallel
            int cores = Runtime.getRuntime().availableProcessors() - 1;
            if (cores < 1) cores = 1;
            Run firstRun = project.getFirstBuild();
            int numberOfRuns = run.getNumber() - firstRun.getNumber();
            int runsPerSegment = 4;
            if (numberOfRuns > cores * 4) {
                runsPerSegment = numberOfRuns / cores;
            }

            ExecutorService server = Executors.newFixedThreadPool(cores);

            // Launch parallel threads
            Run startRun = run;
            Run endRun = startRun;
            do {
                int i = 0;
                while (i < runsPerSegment && endRun != null) {
                    endRun = endRun.getPreviousBuild();
                    i++;
                }
                server.execute(new RunnableJenkinsReader(startRun, endRun, mapper));
                startRun = endRun;
            } while (startRun != null);

            server.shutdown();

            // Blocks until all tasks have completed execution after a shutdown request
            server.awaitTermination(5, TimeUnit.MINUTES);

            return mapper;

        } else {

            MapperBase mapper = new MapperBase(run.getNumber(), truncateStrings);

            // Load condensed file if present
            StringBuffer condensedFilename = new StringBuffer(run.getParent().getRootDir().getAbsolutePath());
            condensedFilename.append(File.separator);
            condensedFilename.append("BenchmarkCondensed.json");
            if (!mapper.importCondensedFromFile(condensedFilename.toString())) {
                return null;
            }

            // Load the files between the current build and the condensed one sequentially
            while (run != null && run.getNumber() != mapper.getBuild()){
                run = run.getPreviousBuild();
            }

            if (run == null) {
                return mapper;
            }

            // Load the files below the condensed one in parallel
            int cores = Runtime.getRuntime().availableProcessors() - 1;
            if (cores < 1) cores = 1;
            Run firstRun = project.getFirstBuild();
            int numberOfRuns = run.getNumber() - firstRun.getNumber();
            int runsPerSegment = 4;
            if (numberOfRuns > cores * 4 ) {
                runsPerSegment = numberOfRuns / cores;
            }

            ExecutorService server = Executors.newFixedThreadPool(cores);

            // Launch parallel threads
            Run startRun = run;
            Run endRun = startRun;
            do{
                int i = 0;
                while (i < runsPerSegment && endRun != null) {
                    endRun = endRun.getPreviousBuild();
                    i++;
                }
                server.execute(new RunnableReader(startRun, endRun, mapper));
                startRun = endRun;
            } while (startRun != null);

            server.shutdown();

            // Blocks until all tasks have completed execution after a shutdown request
            server.awaitTermination(5, TimeUnit.MINUTES);

            return mapper;
        }
    }

    /**
     * fill All Results from files
     * @param project Job being executed
     */
    public void fillAllResults(Job project){
        try {
            resetClock();
            Run run = project.getLastBuild();
            if (run != null) {
                MapperBase base = this.getMapper(run);
                base.setBuild(run.getNumber());
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

    @DataBoundSetter
    public void setThresholds(List<? extends Threshold> thresholds) { this.altThresholds = thresholds; }

    public void setTimer(Timer timer) { this.timer = timer; }
    public void setSelectedResult(Integer selectedResult) { this.selectedResult = selectedResult; }
    public void setSelectedBuild(Integer selectedBuild) { this.selectedBuild = selectedBuild; }
    public void setMapper(MapperBase mapper){ this.map = mapper;}

    // Getters

    public String getInputLocation() { return inputLocation; }
    public String getSchemaSelection() { return schemaSelection; }
    public Boolean getTruncateStrings() { return truncateStrings; }
    public String getAltInputSchema() { return altInputSchema; }
    public String getAltInputSchemaLocation() { return altInputSchemaLocation; }

    public List<? extends Threshold> getThresholds() { return altThresholds; }
    public Timer getTimer() { return timer; }
    public Integer getSelectedResult() { return selectedResult; }
    public Integer getSelectedBuild() { return selectedBuild; }
    public MapperBase getMapper(){ return map; }
    public MapperBase getMapper(Run run) throws NullPointerException, InterruptedException, ValidationException, IOException,  JsonIOException, JsonSyntaxException {
        MapperBase base = this.map;
        if (base == null || run.getNumber() != base.getBuild()) {
            base = this.getRawResults(run);
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
    @Extension @Symbol("benchmark") // This indicates to Jenkins that this is an implementation of an extension point.
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

