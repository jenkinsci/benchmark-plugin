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
package org.jenkinsci.plugins.benchmark.parsers;

import com.google.gson.*;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.benchmark.condensed.DoubleCondensed;
import org.jenkinsci.plugins.benchmark.condensed.IntegerCondensed;
import org.jenkinsci.plugins.benchmark.exceptions.ValidationException;
import org.jenkinsci.plugins.benchmark.results.*;
import org.jenkinsci.plugins.benchmark.thresholds.Threshold;
import org.jenkinsci.plugins.benchmark.utilities.ContentDetected;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.jenkinsci.plugins.benchmark.results.TestValue.FAILED_STATE_COLOR;
import static org.jenkinsci.plugins.benchmark.results.TestValue.PASSED_STATE_COLOR;

/**
 * Base class for the mapper
 * As-Is mapper to load the Jenkins plugin specific format
 * As-Base mapper to load XML or JSON result format
 *
 * @author Daniel Mercier
 * @since 6/20/2017.
 */
public class MapperBase {

    // Variables

    protected final TestGroup               rootGroup;

    protected final Map<Integer, TestGroup> groups = new HashMap<Integer, TestGroup>();
    protected final Map<Integer, TestGroup> files = new ConcurrentHashMap<Integer, TestGroup>();
    protected final Map<Integer, TestValue> results = new ConcurrentHashMap<Integer, TestValue>();
    protected final Map<Integer, TestValue> parameters = new ConcurrentHashMap<Integer, TestValue>();

    protected final char                decimalSeparator;
    protected final boolean             truncateStrings;

    protected boolean hasNumericResult    = false;
    protected boolean hasHistoryThreshold = false;

    private Integer                     build;
    protected ContentDetected           detected;
    protected final TreeSet<Integer>    builds;


    // Constructor

    public MapperBase(Integer build, Boolean truncateStrings){
        this.rootGroup = new TestGroup(null, "__root__", "");
        this.builds = new TreeSet<Integer>();
        this.detected = new ContentDetected();
        this.truncateStrings = truncateStrings;
        this.build = build;

        DecimalFormat format = (DecimalFormat)DecimalFormat.getInstance();
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        this.decimalSeparator = symbols.getDecimalSeparator();
    }

    // Functions


    /**
     * Merge the content from mapper into this MapperBAse
     * @param mapper to be merged in.
     * @throws ValidationException Validation exception
     */
    public void mergeWith(MapperBase mapper) throws ValidationException {

        if (mapper != null && mapper.results.size() != 0) {
            for (Map.Entry<Integer, TestValue> baseResult : mapper.getResults().entrySet()) {
                boolean detectedResult = false;
                for (Map.Entry<Integer,TestValue> result : results.entrySet()) {
                    if (result.getKey().intValue() == baseResult.getKey().intValue()) {
                        TestValue value = result.getValue();
                        switch (value.getType()) {
                            case rt_double:
                                {
                                    DoubleValue dblValue = (DoubleValue) value;
                                    DoubleValue dblBaseValue = (DoubleValue) baseResult.getValue();
                                    Double dblV = dblValue.getValues().get(0);
                                    dblValue.getValues().clear();
                                    dblValue.getValues().put(build, dblV);
                                    dblValue.getValues().putAll(dblBaseValue.getValues());
                                    if (dblValue.getProperties().size() > 0) {
                                        TestProperty dblProperty = dblValue.getProperties().get(0);
                                        dblValue.getProperties().clear();
                                        dblValue.getProperties().put(build, dblProperty);
                                    }
                                    dblValue.getProperties().putAll(dblBaseValue.getProperties());
                                }
                                break;
                            case rt_integer:
                                {
                                    IntegerValue intValue = (IntegerValue) value;
                                    IntegerValue intBaseValue = (IntegerValue) baseResult.getValue();
                                    Integer intV = intValue.getValues().get(0);
                                    intValue.getValues().clear();
                                    intValue.getValues().put(build, intV);
                                    intValue.getValues().putAll(intBaseValue.getValues());
                                    if (intValue.getProperties().size() > 0) {
                                        TestProperty intProperty = intValue.getProperties().get(0);
                                        intValue.getProperties().clear();
                                        intValue.getProperties().put(build, intProperty);
                                    }
                                    intValue.getProperties().putAll(intBaseValue.getProperties());
                                }
                                break;
                            case rt_boolean:
                                {
                                    BooleanValue boolValue = (BooleanValue) value;
                                    BooleanValue boolBaseValue = (BooleanValue) baseResult.getValue();
                                    Boolean boolV = boolValue.getValues().get(0);
                                    boolValue.getValues().clear();
                                    boolValue.getValues().put(build, boolV);
                                    boolValue.getValues().putAll(boolBaseValue.getValues());
                                    if (boolValue.getProperties().size() > 0) {
                                        TestProperty boolProperty = boolValue.getProperties().get(0);
                                        boolValue.getProperties().clear();
                                        boolValue.getProperties().put(build, boolProperty);
                                    }
                                    boolValue.getProperties().putAll(boolBaseValue.getProperties());
                                }
                                break;
                            case rt_string:
                                {
                                    StringValue strValue = (StringValue) value;
                                    StringValue strBaseValue = (StringValue) baseResult.getValue();
                                    String strV = strValue.getValues().get(0);
                                    strValue.getValues().clear();
                                    strValue.getValues().put(build, strV);
                                    strValue.getValues().putAll(strBaseValue.getValues());
                                    if (strValue.getProperties().size() > 0) {
                                        TestProperty strProperty = strValue.getProperties().get(0);
                                        strValue.getProperties().clear();
                                        strValue.getProperties().put(build, strProperty);
                                    }
                                    strValue.getProperties().putAll(strBaseValue.getProperties());
                                }
                                break;
                            default:
                        }
                        detectedResult = true;
                        break;
                    }
                }
                if (!detectedResult) {
                    results.put(baseResult.getKey(), baseResult.getValue());
                }
            }
            for (Map.Entry<Integer, TestValue> baseParam : mapper.getParameters().entrySet()) {
                boolean detectedParam = false;
                for (Map.Entry<Integer,TestValue> param : parameters.entrySet()) {
                    if (param.getKey().intValue() == baseParam.getKey().intValue()) {
                        TestValue value = param.getValue();
                        switch (value.getType()) {
                            case rt_double:
                            {
                                DoubleValue dblValue = (DoubleValue) value;
                                DoubleValue dblBaseValue = (DoubleValue) baseParam.getValue();
                                Double dblV = dblValue.getValues().get(0);
                                dblValue.getValues().clear();
                                dblValue.getValues().put(build, dblV);
                                dblValue.getValues().putAll(dblBaseValue.getValues());
                            }
                            break;
                            case rt_integer:
                            {
                                IntegerValue intValue = (IntegerValue) value;
                                IntegerValue intBaseValue = (IntegerValue) baseParam.getValue();
                                Integer intV = intValue.getValues().get(0);
                                intValue.getValues().clear();
                                intValue.getValues().put(build, intV);
                                intValue.getValues().putAll(intBaseValue.getValues());
                            }
                            break;
                            case rt_boolean:
                            {
                                BooleanValue boolValue = (BooleanValue) value;
                                BooleanValue boolBaseValue = (BooleanValue) baseParam.getValue();
                                Boolean boolV = boolValue.getValues().get(0);
                                boolValue.getValues().clear();
                                boolValue.getValues().put(build, boolV);
                                boolValue.getValues().putAll(boolBaseValue.getValues());
                            }
                            break;
                            case rt_string:
                            {
                                StringValue strValue = (StringValue) value;
                                StringValue strBaseValue = (StringValue) baseParam.getValue();
                                String strV = strValue.getValues().get(0);
                                strValue.getValues().clear();
                                strValue.getValues().put(build, strV);
                                strValue.getValues().putAll(strBaseValue.getValues());
                            }
                            break;
                            default:
                        }
                        detectedParam = true;
                        break;
                    }
                }
                if (!detectedParam) {
                    results.put(baseParam.getKey(), baseParam.getValue());
                }
            }
            for (Map.Entry<Integer, TestGroup> baseFile : mapper.getFiles().entrySet()) {
                boolean detectedFile = false;
                for (Map.Entry<Integer,TestGroup> file : files.entrySet()) {
                    if (file.getKey().intValue() == baseFile.getKey().intValue()) {
                        detectedFile = true;
                        break;
                    }
                }
                if (!detectedFile) {
                    files.put(baseFile.getKey(), baseFile.getValue());
                }
            }
        }
    }

    /**
     * Add a list of thresholds at the right location inside the tree.
     * @param thresholds List of additional thresholds to add to the mapper content
     */
    public void addAllThresholds (List<? extends Threshold> thresholds){
        for( Threshold threshold:thresholds){
            addThreshold(threshold);
        }
    }

    /**
     * Add a threshold at the right location.
     * @param threshold Threshold to add
     */
    private void addThreshold (Threshold threshold){
        String extName = "";
        if (threshold.getTestGroup().isEmpty()) {
            if (threshold.getTestName().isEmpty()) {
                for (TestValue result : results.values()) {
                    result.addThreshold(threshold);
                    checkThresholdType(threshold);
                }
            } else {
                for (TestValue result : results.values()) {
                    if (result.getName().equals(threshold.getTestName())) {
                        result.addThreshold(threshold);
                        checkThresholdType(threshold);
                    }
                }
            }
        } else {
            extName = threshold.getTestGroup();
            if(!threshold.getTestName().isEmpty()) {
                extName += "." + threshold.getTestName();
            }
            Integer hash =  extName.hashCode();
            for (Map.Entry<Integer,TestGroup> group:groups.entrySet()){
                TestGroup grp = group.getValue();
                if (grp.getClassType() != TestGroup.ClassType.ct_fileGrp) {
                    Integer grpHash = grp.getFileSubGroupFullName().hashCode();
                    if (hash.intValue() == grpHash.intValue()) {
                        grp.addThreshold(threshold);
                        checkThresholdType(threshold);
                    }
                }
            }
        }
    }

    /**
     * Determine threshold for each results
     * @param base Mapper storing results to check
     * @return Boolean whether thresholds were crossed or not
     */
    public boolean checkThresholds(MapperBase base)  {
        boolean failed = false;
        for (Map.Entry<Integer,TestValue> result : results.entrySet()) {
            if (base != null && base.results.size() != 0) {
                if (result.getValue().getType() == TestValue.ValueType.rt_double) {
                    for (Map.Entry<Integer, TestValue> baseResult : base.getResults().entrySet()) {
                        if (result.getKey().intValue() == baseResult.getKey().intValue()) {
                            DoubleCondensed value = (DoubleCondensed) baseResult.getValue();
                            result.getValue().checkThresholdStatus(value.getPrevious(), value.getAverage());
                            break;
                        }
                    }
                } else if (result.getValue().getType() == TestValue.ValueType.rt_integer) {
                    for (Map.Entry<Integer, TestValue> baseResult : base.getResults().entrySet()) {
                        if (result.getKey().intValue() == baseResult.getKey().intValue()) {
                            IntegerCondensed value = (IntegerCondensed) baseResult.getValue();
                            result.getValue().checkThresholdStatus(value.getPrevious().doubleValue(), value.getAverage());
                            break;
                        }
                    }
                }
            } else {
                result.getValue().checkThresholdStatus(null, null);
            }

            // Check final fail state
            Boolean state = result.getValue().getFailedState();
            if (state != null && state == true){
                failed = true;
            }
        }
        return failed;
    }

    /**
     * Import data from Jenkins stored file using String filename
     *
     * @param inputFile File name to be imported
     * @throws NullPointerException If null pointer detected
     * @throws FileNotFoundException If file not found
     * @throws JsonIOException If I/O errors occur
     * @throws JsonSyntaxException If JSON syntax invalid
     */
    public void importFromFile (String inputFile) throws NullPointerException, IOException, JsonIOException, JsonSyntaxException {
        File oFile = new File(inputFile);
        if (oFile.exists()) {
            this.importFromFile(oFile, detected);
        }
    }

    /**
     * Import data from Jenkins stored file using Java File
     *
     * @param inputFile File name to be imported
     * @param detected Key characteristics about the set of results
     * @throws NullPointerException If null pointer detected
     * @throws FileNotFoundException If file not found
     * @throws JsonIOException If I/O errors occur
     * @throws JsonSyntaxException If JSON syntax invalid
     */
    public void importFromFile (File inputFile, ContentDetected detected) throws NullPointerException, IOException, JsonIOException, JsonSyntaxException {

        Integer build = null;
        JsonParser parser = new JsonParser();
        InputStreamReader reader = new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8);
        JsonElement jsonContent = parser.parse(reader);

        if (jsonContent.isJsonObject()){
            JsonObject jsonObject = jsonContent.getAsJsonObject();

            // Load base information
            for (Map.Entry<String, JsonElement> enContent : jsonObject.entrySet()) {
                if (enContent.getKey().equalsIgnoreCase("build")) {
                    JsonElement element = enContent.getValue();
                    if (element.isJsonPrimitive()) {
                        JsonPrimitive primitive = element.getAsJsonPrimitive();
                        if (primitive.isNumber()) {
                            build = primitive.getAsInt();
                            this.build = build;
                            this.builds.add(build);
                        }
                    }
                    break;
                }
            }

            if (build != null) {
                // Load parameters
                for (Map.Entry<String, JsonElement> enContent : jsonObject.entrySet()) {
                    if (enContent.getKey().equalsIgnoreCase("parameters")) {
                        JsonElement element = enContent.getValue();
                        if (element.isJsonArray()) {
                            JsonArray array = element.getAsJsonArray();
                            for (JsonElement aElement : array) {
                                if (aElement.isJsonObject()) {
                                    JsonObject aObject = aElement.getAsJsonObject();
                                    TestValue.convertParameterJsonObject(build, aObject, rootGroup, parameters);
                                }
                            }
                        }
                    }
                }

                // Load results
                for (Map.Entry<String, JsonElement> enContent : jsonObject.entrySet()) {
                    if (enContent.getKey().equalsIgnoreCase("results")) {
                        JsonElement element = enContent.getValue();
                        if (element.isJsonArray()) {
                            JsonArray array = element.getAsJsonArray();
                            for (JsonElement aElement : array) {
                                if (aElement.isJsonObject()) {
                                    JsonObject aObject = aElement.getAsJsonObject();
                                    TestValue.convertResultJsonObject(build, aObject, rootGroup, files, results, parameters);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Export data to Jenkins stored file
     * @param outputFile Output file
     * @param job Job name
     * @param build  Build number
     * @return Whether export succeeded or not
     */
    public boolean exportToFile (String outputFile, String job, int build) {
        try {
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8);

            JsonObject rootObject = new JsonObject();
            rootObject.addProperty("job", job);
            rootObject.addProperty("build", build);

            if (results.size() > 0) {
                JsonArray resultArray = new JsonArray();
                for (Map.Entry<Integer, TestValue> result : results.entrySet()) {
                    resultArray.add(result.getValue().getJsonObject(result.getKey()));
                }
                rootObject.add("results", resultArray);
            }

            if (parameters.size() > 0) {
                JsonArray parameterArray = new JsonArray();
                for (Map.Entry<Integer, TestValue> parameter : parameters.entrySet()) {
                    parameterArray.add(parameter.getValue().getJsonObject(parameter.getKey()));
                }
                rootObject.add("parameters", parameterArray);
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(rootObject, writer);
            writer.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Get the HTML content to represent the result in a table [TABLE PAGE]
     * @return HTML content to represent the result
     */
    public String getHTMLTable(){
        if (results.size() == 0) {
            return "";
        }

        int nBuilds = this.builds.last() - this.builds.first() + 1;
        List<Integer> listNFailed = new ArrayList<Integer>(Collections.nCopies(nBuilds,0));
        List<Integer> listNPassed = new ArrayList<Integer>(Collections.nCopies(nBuilds,0));

        // Body
        StringBuffer body = new StringBuffer();
        body.append("<tbody>");
        for (Map.Entry<Integer, TestValue> result:results.entrySet()){
            body.append(result.getValue().getHTMLResult(result.getKey(), detected, builds, listNPassed, listNFailed, decimalSeparator));
        }
        body.append("</tbody>");

        // Headers
        StringBuffer header_top = new StringBuffer();
        StringBuffer header_bot = new StringBuffer();
        header_top.append("<thead><tr>");
        header_bot.append("<tfoot><tr>");
        if (detected.isFileDetected()) {
            header_top.append("<th rowspan=\"3\">");
            header_top.append(Messages.MapperBase_Location());
            header_top.append("</th>");
            header_bot.append("<th rowspan=\"3\">");
            header_bot.append(Messages.MapperBase_Location());
            header_bot.append("</th>");
        }
        if (detected.isGroupDetected()){

            header_top.append("<th rowspan=\"3\">");
            header_top.append(Messages.MapperBase_Group());
            header_top.append("</th>");
            header_bot.append("<th rowspan=\"3\">");
            header_bot.append(Messages.MapperBase_Group());
            header_bot.append("</th>");
        }

        // Top
        if (detected.isUnitsDetected()) {
            header_top.append("<th rowspan=\"3\">");
            header_top.append(Messages.MapperBase_Result());
            header_top.append("</th>");
        }
        header_top.append("<th>");
        header_top.append(Messages.MapperBase_Passed());
        header_top.append("</th><th rowspan=\"3\">");
        header_top.append(Messages.MapperBase_ResultKey());
        for(Integer nPassed:listNPassed){
            header_top.append("</th><th style=\"background-color:");
            header_top.append(PASSED_STATE_COLOR);
            header_top.append(";\">");
            header_top.append(nPassed.toString());
        }
        header_top.append("</th></tr><tr><th>");
        header_top.append(Messages.MapperBase_Failed());
        for(Integer nFailed:listNFailed){
            header_top.append("</th><th style=\"background-color:");
            header_top.append(FAILED_STATE_COLOR);
            header_top.append(";\">");
            header_top.append(nFailed.toString());
        }
        header_top.append("</th></tr><tr><th>");
        if (detected.isUnitsDetected()) {
            header_top.append(Messages.MapperBase_Unit());
        } else {
            header_top.append(Messages.MapperBase_Result());
        }
        for (Integer build = this.builds.last(); build >= this.builds.first(); build--) {
            header_top.append("</th><th>");
            header_top.append(build.toString());
        }
        header_top.append("</th></tr></thead>");

        // Bottom
        if (detected.isUnitsDetected()) {
            header_bot.append("<th rowspan=\"3\">");
            header_bot.append(Messages.MapperBase_Result());
            header_bot.append("</th><th>");
            header_bot.append(Messages.MapperBase_Unit());
        } else {
            header_bot.append("<th>");
            header_bot.append(Messages.MapperBase_Result());
        }
        header_bot.append("</th><th rowspan=\"3\">");
        header_bot.append(Messages.MapperBase_ResultKey());
        for (Integer build = this.builds.last(); build >= this.builds.first(); build--) {
            header_bot.append("</th><th>");
            header_bot.append(build.toString());
        }
        header_bot.append("</th></tr><tr><th>");
        header_bot.append(Messages.MapperBase_Passed());
        for(Integer nPassed:listNPassed){
            header_bot.append("</th><th style=\"background-color:");
            header_bot.append(PASSED_STATE_COLOR);
            header_bot.append(";\">");
            header_bot.append(nPassed.toString());
        }
        header_bot.append("</th></tr><tr><th>");
        header_bot.append(Messages.MapperBase_Failed());
        for(Integer nFailed:listNFailed){
            header_bot.append("</th><th style=\"background-color:");
            header_bot.append(FAILED_STATE_COLOR);
            header_bot.append(";\">");
            header_bot.append(nFailed.toString());
        }
        header_bot.append("</th></tr></tfoot>");

        // Final assembly
        header_top.append(header_bot);
        header_top.append(body);
        return header_top.toString();
    }

    /**
     * Get the CSV Table header of the raw table [CSV EXPORT]
     * @return CSV content to represent the result
     */
    public String getCSVTableHeader(){
        if (results.size() == 0) {
            return "";
        }
        StringBuffer content = new StringBuffer();

        if (detected.isFileDetected()) {
            content.append(Messages.MapperBase_Location());
            content.append(',');
        }
        if (detected.isGroupDetected()){
            content.append(Messages.MapperBase_Group());
            content.append(',');
        }
        content.append(Messages.MapperBase_Result());
        for (Integer build = this.builds.last(); build >= this.builds.first(); build--) {
            content.append(',');
            content.append(build.toString());
        }
        return content.toString();
    }

    /**
     * Get the CSV Table body of the raw table [CSV EXPORT]
     * @return CSV content to represent the result
     */
    public String getCSVTableBody(){
        if (results.size() == 0) {
            return "";
        }

        StringBuffer content = new StringBuffer();
        for (TestValue result:results.values()){
            content.append(result.getCSVResult(builds, detected));
            content.append("\n");
        }
        return content.toString();
    }

    /**
     * Get the CSV Table body of the raw state table [CSV EXPORT]
     * @return CSV content to represent the result
     */
    public String getCSVTableStateBody(){
        if (results.size() == 0) {
            return "";
        }

        StringBuffer content = new StringBuffer();
        for (Map.Entry<Integer, TestValue> result:results.entrySet()){
            content.append(result.getValue().getCSVResultState(result.getKey(), detected, builds));
            content.append("\n");
        }
        return content.toString();
    }

    // Condensed content

    /**
     * Generate HTML table header + content [TABLE PAGE]
     * @return HTML content for condensed table
     */
    public String getHTMLCondensedTable(){
        if (results.size() == 0) {
            return "";
        }
        StringBuffer header = new StringBuffer();
        header.append("<th>");
        if (detected.isFileDetected()) {
            header.append(Messages.MapperBase_Location());
            header.append("</th><th>");
        }
        if (detected.isGroupDetected()){
            header.append(Messages.MapperBase_Group());
            header.append("</th><th>");
        }
        header.append(Messages.MapperBase_Result());
        header.append("</th><th>");
        if (detected.isUnitsDetected()){
            header.append(Messages.MapperBase_Unit());
            header.append("</th><th>");
        }
        header.append(Messages.MapperBase_ResultKey());
        header.append("</th><th>");
        if (detected.isNumeralDetected()){
            header.append(Messages.MapperBase_Minimum());
            header.append("</th><th>");
            header.append(Messages.MapperBase_Maximum());
            header.append("</th><th>");
            header.append(Messages.MapperBase_Average());
            header.append("</th><th>");
            header.append(Messages.MapperBase_StdDeviation());
            header.append("</th><th>");
        }
        header.append(Messages.MapperBase_Passed());
        header.append("</th><th>");
        header.append(Messages.MapperBase_Failed());
        header.append("</th>");

        StringBuffer content = new StringBuffer();
        content.append("<thead><tr>");
        content.append(header);
        content.append("</tr></thead><tfoot><tr>");
        content.append(header);
        content.append("</tr></tfoot><tbody>");

        for (Map.Entry<Integer, TestValue> result:results.entrySet()){
            content.append(result.getValue().getHTMLCondensed(result.getKey(), detected, decimalSeparator));
        }
        content.append("</tbody>");
        return content.toString();
    }

    /**
     * Generate CSV table header for condensed content [CSV EXPORT]
     * @return CSV header content for condensed table
     */
    public String getCSVCondensedTableHeader() {
        if (results.size() == 0) {
            return "";
        }
        StringBuffer content = new StringBuffer();
        if (detected.isFileDetected()) {
            content.append(Messages.MapperBase_Location());
            content.append(',');
        }
        if (detected.isGroupDetected()) {
            content.append(Messages.MapperBase_Group());
            content.append(',');
        }
        content.append(Messages.MapperBase_Result());
        if (detected.isUnitsDetected()){
            content.append(',');
            content.append(Messages.MapperBase_Unit());
        }
        if (detected.isNumeralDetected()) {
            content.append(',');
            content.append(Messages.MapperBase_Minimum());
            content.append(',');
            content.append(Messages.MapperBase_Maximum());
            content.append(',');
            content.append(Messages.MapperBase_Average());
            content.append(',');
            content.append(Messages.MapperBase_StdDeviation());
        }
        content.append(',');
        content.append(Messages.MapperBase_Failed());
        content.append(',');
        content.append(Messages.MapperBase_Passed());
        return content.toString();
    }

    /**
     * Generate CSV table body for condensed content [CSV EXPORT]
     * @return CSV body content for condensed table
     */
    public String getCSVCondensedTableBody() {
        if (results.size() == 0) {
            return "";
        }
        StringBuffer content = new StringBuffer();
        for (TestValue result:results.values()){
            content.append(result.getCSVCondensed(detected));
            content.append("\n");
        }
        return content.toString();
    }

    /**
     * Import condensed results from file
     * @param inputFilename File name
     * @throws NullPointerException If null pointer detected
     * @throws FileNotFoundException If file not found
     * @throws JsonIOException If I/O errors occur
     * @throws JsonSyntaxException If JSON syntax invalid
     * @throws ValidationException If validation failed
     * @return True if file detected and loaded.
     */
    public boolean importCondensedFromFile (String inputFilename) throws NullPointerException, IOException, JsonIOException, JsonSyntaxException, ValidationException {
        File inputFile = new File(inputFilename);
        if (inputFile.exists()) {
            this.importCondensedFromFile(inputFile);
            return true;
        }
        return false;
    }

    /**
     * Import condensed results from file
     * @param inputFile Input file
     * @throws NullPointerException If null pointer detected
     * @throws FileNotFoundException If file not found
     * @throws JsonIOException If I/O errors occur
     * @throws JsonSyntaxException If JSON syntax invalid
     * @throws ValidationException If validation failed
     */
    public void importCondensedFromFile (File inputFile) throws NullPointerException, IOException, JsonIOException, JsonSyntaxException, ValidationException {

        Integer build = null;
        JsonParser parser = new JsonParser();
        InputStreamReader reader = new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8);
        JsonElement jsonContent = parser.parse(reader);

        if (jsonContent.isJsonObject()) {
            JsonObject jsonObject = jsonContent.getAsJsonObject();

            // Load base information
            for (Map.Entry<String, JsonElement> enContent : jsonObject.entrySet()) {
                if (enContent.getKey().equalsIgnoreCase("build")) {
                    JsonElement element = enContent.getValue();
                    if (element.isJsonPrimitive()) {
                        JsonPrimitive primitive = element.getAsJsonPrimitive();
                        if (primitive.isNumber()) {
                            build = primitive.getAsInt();
                        }
                    }
                    break;
                }
            }

            if (build != null) {
                // Load file groups
                for (Map.Entry<String, JsonElement> enContent : jsonObject.entrySet()) {
                    if (enContent.getKey().equalsIgnoreCase("files")) {
                        JsonElement element = enContent.getValue();
                        if (element.isJsonArray()) {
                            JsonArray array = element.getAsJsonArray();
                            for (JsonElement aElement : array) {
                                if (aElement.isJsonObject()) {
                                    JsonObject aObject = aElement.getAsJsonObject();
                                    TestGroup.convertCondensedFileJsonObject(aObject, rootGroup, files, detected);
                                }
                            }
                        }
                    }
                }

                // Load parameters
                for (Map.Entry<String, JsonElement> enContent : jsonObject.entrySet()) {
                    if (enContent.getKey().equalsIgnoreCase("parameters")) {
                        JsonElement element = enContent.getValue();
                        if (element.isJsonArray()) {
                            JsonArray array = element.getAsJsonArray();
                            for (JsonElement aElement : array) {
                                if (aElement.isJsonObject()) {
                                    JsonObject aObject = aElement.getAsJsonObject();
                                    TestValue.convertCondensedParameterJsonObject(aObject, rootGroup, parameters, detected);
                                }
                            }
                        }
                    }
                }

                // Load results
                for (Map.Entry<String, JsonElement> enContent : jsonObject.entrySet()) {
                    if (enContent.getKey().equalsIgnoreCase("results")) {
                        JsonElement element = enContent.getValue();
                        if (element.isJsonArray()) {
                            JsonArray array = element.getAsJsonArray();
                            for (JsonElement aElement : array) {
                                if (aElement.isJsonObject()) {
                                    JsonObject aObject = aElement.getAsJsonObject();
                                    TestValue.convertCondensedResultJsonObject(aObject, rootGroup, files, results, detected);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Export condensed results to file
     * @param outputFile    Output file
     * @param job           Job name
     * @param build         Build number
     * @return Whether export was successful
     */
    public boolean exportCondensedToFile(String outputFile, String job, int build) {
        try {
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8);

            JsonObject rootObject = new JsonObject();
            rootObject.addProperty("job", job);
            rootObject.addProperty("build", build);

            if (results.size() > 0) {
                JsonArray resultArray = new JsonArray();
                for (Map.Entry<Integer,TestValue> result : results.entrySet()) {
                    resultArray.add(result.getValue().getCondensedJsonObject(build, result.getKey()));
                }
                rootObject.add("results", resultArray);
            }

            if (parameters.size() > 0) {
                JsonArray parameterArray = new JsonArray();
                for (Map.Entry<Integer, TestValue> parameter : parameters.entrySet()) {
                    parameterArray.add(parameter.getValue().getParameterJsonObject(parameter.getKey()));
                }
                rootObject.add("parameters", parameterArray);
            }

            boolean detFiles = false;
            JsonArray fileArray = new JsonArray();
            for (Map.Entry<Integer, TestGroup> group : groups.entrySet()) {
                if (group.getValue().getClassType() == TestGroup.ClassType.ct_fileGrp) {
                    fileArray.add(group.getValue().getJsonObject(group.getKey()));
                    detFiles = true;
                }
            }
            if (detFiles) {
                rootObject.add("files", fileArray);
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(rootObject, writer);
            writer.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Detect if the result is a numeric
     * @param result Result to test
     */
    protected void checkResult (TestValue result){
        switch (result.getType()) {
            case rt_double:
            case rt_integer:
                this.hasNumericResult = true;
                break;
            default:
        }
    }

    /**
     * Detect if threshold requires history
     * @param threshold Threshold to test
     */
    protected void checkThresholdType( Threshold threshold){
        switch (threshold.getType()) {
            case tt_delta:
            case tt_percentage:
            case tt_deltaAverage:
            case tt_percentageAverage:
                this.hasHistoryThreshold = true;
                break;
            default:
        }
    }

    /**
     * Determine if active thresholds require build history to be validated.
     * @return Whether active thresholds require build history
     */
    public boolean requiresHistory(){
        if (hasNumericResult && hasHistoryThreshold) {
            return true;
        } else {
            return false;
        }
    }

    public void logKeyData(TaskListener listener, Integer numberOfAddedThresholds){
        listener.getLogger().println(Messages.MapperBase_NumberOfResults() + this.getResults().size());
        listener.getLogger().println(Messages.MapperBase_NumberOfParameters() + this.getParameters().size());
        listener.getLogger().println(Messages.MapperBase_NumberOfAddedThresholds() + numberOfAddedThresholds);
        if (this.hasNumericResult) {
            listener.getLogger().println(Messages.MapperBase_ResultsContainsNumerals());
        } else {
            listener.getLogger().println(Messages.MapperBase_ResultsDoesNotContainNumerals());
        }
        if (this.truncateStrings) {
            listener.getLogger().println(Messages.MapperBase_StringsAreTruncated());
        } else {
            listener.getLogger().println(Messages.MapperBase_StringsAreNotTruncated());
        }
    }

    // Setter

    public void setBuild(Integer build) { this.build = build; }

    // Getter

    public TestGroup getRootGroup() { return rootGroup; }

    public int getNumberOfResults() { return results.size(); }
    public Map<Integer, TestValue> getResults() { return results; }

    public int getNumberOfParameters() { return parameters.size(); }
    public Map<Integer, TestValue> getParameters() { return parameters; }

    public int getNumberOfFiles() { return files.size(); }
    public Map<Integer, TestGroup> getFiles() { return files; }

    public int getNumberOfGroups() { return groups.size(); }
    public Map<Integer, TestGroup> getGroups() { return groups; }

    public TreeSet<Integer> getBuilds() { return builds; }

    public ContentDetected getDetected() { return detected; }
    public char getDecimalSeparator() { return decimalSeparator; }

    public Integer getBuild() { return build; }
}
