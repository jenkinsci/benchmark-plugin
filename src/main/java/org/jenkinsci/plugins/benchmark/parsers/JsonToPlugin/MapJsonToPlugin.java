/**
 * MIT License
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
package org.jenkinsci.plugins.benchmark.parsers.JsonToPlugin;

import com.google.gson.*;
import hudson.FilePath;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.benchmark.exceptions.ValidationException;
import org.jenkinsci.plugins.benchmark.parsers.MapperBase;
import org.jenkinsci.plugins.benchmark.results.StringValue;
import org.jenkinsci.plugins.benchmark.results.TestGroup;
import org.jenkinsci.plugins.benchmark.results.TestValue;
import org.jenkinsci.plugins.benchmark.thresholds.Threshold;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Parser from JSON to the Jenkins plugin data construct
 *
 * @author Daniel Mercier
 * @since 6/20/2017.
 */
public class MapJsonToPlugin extends MapperBase {

    // Enumeration

    enum GroupTags {
        gt_unknown,
        gt_object,
        gt_array,
        gt_result,
        gt_booleankey,
        gt_threshold,
        gt_parameter,
    }

    // Constructor

    public MapJsonToPlugin(Integer build, File content, JsonElement schema, boolean truncateStrings) throws IOException, ValidationException{
        super(build, truncateStrings);

        JsonElement jContent;
        try{
            jContent = getJSON(content);
        } catch(Exception e) {
            throw new IOException(Messages.MapJsonToPlugin_FileFormatNotRecognisedAsJson(content.getName()));
        }

        InitiateLoading(rootGroup, jContent, schema);
    }

    public MapJsonToPlugin(Integer build, FilePath content, JsonElement schema, boolean truncateStrings) throws IOException, ValidationException {
        super(build, truncateStrings);

        JsonElement jContent;
        try{
            jContent = getJSON(content);
        } catch(Exception e) {
            throw new IOException(Messages.MapJsonToPlugin_FileFormatNotRecognisedAsJson(content.getName()));
        }

        InitiateLoading(rootGroup, jContent, schema);
    }

    public MapJsonToPlugin(Integer build, Map<String, FilePath> content, JsonElement schema, boolean truncateStrings, TaskListener listener) throws IOException, ValidationException {
        super(build, truncateStrings);

        listener.getLogger().println(Messages.MapJsonToPlugin_ListOfFilesDetected());

        JsonElement jContent;
        int files_processed = 0;
        for(Map.Entry<String, FilePath> entry:content.entrySet()) {

            String relativePath = FilePathToString(entry.getValue());

            try {
                jContent = getJSON(entry.getValue());
            } catch (Exception e) {
                listener.getLogger().println("   - " + Messages.MapJsonToPlugin_PrintFailedToIdentifyFile(relativePath));
                continue;
            }

            try {
                TestGroup group = new TestGroup(rootGroup, entry.getKey(), relativePath, TestValue.ClassType.ct_fileGrp);
                files.put(group.getGroupHash(), group);
                groups.put(group.getGroupHash(), group);
                rootGroup.addGroup(group);

                InitiateLoading(group, jContent, schema);
                listener.getLogger().println("   - " + relativePath);
            } catch (Exception e){
                listener.getLogger().println("   - " + Messages.MapJsonToPlugin_PrintFailedToLoadFile(relativePath));
                continue;
            }
            listener.getLogger().println("   - " + relativePath);
            files_processed++;
        }
        if (files_processed == 0) {
            throw new ValidationException(Messages.MapJsonToPlugin_NoValidFileFound());
        }
    }

    public MapJsonToPlugin(Integer build, FilePath content, String schema, boolean truncateStrings) throws IOException, ValidationException {
        super(build, truncateStrings);

        JsonElement jSchema;
        try {
            jSchema = getJSON(schema);
        } catch (Exception e) {
            throw new IOException(Messages.MapJsonToPlugin_SchemaNotRecognisedAsJson());
        }

        JsonElement jContent;

        try{
            jContent = getJSON(content);
        } catch(Exception e) {
            throw new IOException(Messages.MapJsonToPlugin_FileFormatNotRecognisedAsJson(content.getName()));
        }

        InitiateLoading(rootGroup, jContent, jSchema);
    }

    public MapJsonToPlugin(Integer build, Map<String, FilePath> content, String schema, boolean truncateStrings) throws IOException, ValidationException {
        super(build, truncateStrings);

        JsonElement jSchema;
        try {
            jSchema = getJSON(schema);
        } catch (Exception e) {
            throw new IOException(Messages.MapJsonToPlugin_SchemaNotRecognisedAsJson());
        }

        JsonElement jContent;
        for(Map.Entry<String, FilePath> entry:content.entrySet()) {
            try {
                jContent = getJSON(entry.getValue());
            } catch (Exception e) {
                throw new IOException(Messages.MapJsonToPlugin_FileFormatNotRecognisedAsJson(entry.getValue().getName()));
            }

            FilePath path = entry.getValue();
            String relativePath = null;
            String nextChunk = null;
            while (path != null && !path.getName().equalsIgnoreCase("workspace")) {
                if (relativePath == null) {
                    if (nextChunk != null) {
                        relativePath = nextChunk;
                    }
                } else {
                    relativePath = nextChunk + "/" + relativePath;
                }
                nextChunk = path.getName();
                path = path.getParent();
            }

            TestGroup group = new TestGroup(rootGroup, entry.getKey(), relativePath, TestValue.ClassType.ct_fileGrp);
            files.put(group.getGroupHash(), group);
            groups.put(group.getGroupHash(), group);
            rootGroup.addGroup(group);

            InitiateLoading(group, jContent, jSchema);
        }
    }

    // Functions

    /**
     * Determine if a file is a valid JSON.
     * @param jsonFile JSON file
     * @return Json Element in GSON format
     */
    private JsonElement getJSON(File jsonFile) throws IOException, InterruptedException, JsonIOException, JsonSyntaxException {
        JsonParser parser = new JsonParser();
        JsonElement content = parser.parse(new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8));
        return content;
    }

    /**
     * Determine if a file is a valid JSON.
     * @param jsonFile JSON file
     * @return Json Element in GSON format
     */
    private JsonElement getJSON(FilePath jsonFile) throws IOException, InterruptedException, JsonIOException, JsonSyntaxException {
        JsonParser parser = new JsonParser();
        JsonElement content = parser.parse(new InputStreamReader(jsonFile.read(), StandardCharsets.UTF_8));
        return content;
    }

    /**
     * Determine if a string is a valid JSON.
     * @param sContent Json content in string format
     * @return Json Element in GSON format
     */
    private JsonElement getJSON(String sContent) throws JsonIOException, JsonSyntaxException{
        JsonParser parser = new JsonParser();
        JsonElement content = parser.parse(sContent);
        return content;
    }

    /**
     * Initiate the loading of file content
     * @param eContent Content from result file
     * @param eSchema Content from schema file
     * @throws ValidationException If validation error occur
     */
    private void InitiateLoading(TestGroup group, JsonElement eContent, JsonElement eSchema) throws ValidationException {
        ProcessBlock(group, "__first__", eContent, eSchema, null);
    }

    /**
     * Process block of data
     * @param parent Pointer to parent group
     * @param key Key associated with content from result file
     * @param eContent Content from result file
     * @param eSchema Content from schema file
     * @param oldFailures Previously registered failures
     * @throws ValidationException If validation error occur
     */
    private void ProcessBlock(TestGroup parent, String key, JsonElement eContent, JsonElement eSchema, MapJsonFailures oldFailures) throws ValidationException {

        if (eSchema.isJsonObject()) {
            JsonObject oSchema = eSchema.getAsJsonObject();

            // Load boolean failure modes
            MapJsonFailures failures = new MapJsonFailures(parent, eSchema, oldFailures);

            // Switchboard
            GroupTags type = getGroupTag(oSchema);
            switch (type) {
                case gt_result:
                    boolean detResult = false;
                    for (Map.Entry<String, JsonElement> entry : oSchema.entrySet()) {
                        if (entry.getKey().equals("properties")) {
                            ProcessResult(parent, key, eContent, entry.getValue(), failures);
                            detResult = true;
                            break;
                        }
                    }
                    if (!detResult) {
                        ProcessResultFull (parent, key, eContent, eSchema, failures, false);
                    }
                    break;

                case gt_booleankey:
                    ProcessResultFull (parent, key, eContent, eSchema, failures, true);
                    break;

                case gt_threshold:
                    for (Map.Entry<String, JsonElement> entry : oSchema.entrySet()) {
                        if (entry.getKey().equals("properties")) {
                            ProcessThreshold(parent, key, eContent, entry.getValue(), failures);
                            break;
                        }
                    }
                    break;

                case gt_parameter:
                    boolean detParameter = false;
                    for (Map.Entry<String, JsonElement> entry : oSchema.entrySet()) {
                        if (entry.getKey().equals("properties")) {
                            ProcessParameter(parent, key, eContent, entry.getValue(), failures);
                            detParameter = true;
                            break;
                        }
                    }
                    if (!detParameter) {
                        ProcessParameterFull (parent, key, eContent, eSchema, failures);
                    }
                    break;

                case gt_object:
                    for (Map.Entry<String, JsonElement> entry : oSchema.entrySet()) {
                        if (entry.getKey().equals("properties")) {
                            ProcessObject(parent, key, eContent, entry.getValue(), failures);
                            break;
                        }
                    }
                    break;

                case gt_array:
                    for (Map.Entry<String, JsonElement> entry : oSchema.entrySet()) {
                        if (entry.getKey().equals("items")) {
                            ProcessArray(parent, key, eContent, entry.getValue(), failures);
                            break;
                        }
                    }
                    break;
            }
        }
    }

    /**
     *  Read the schema for a threshold and load its related content.
     *
     * @param parent Pointer to parent group
     * @param key Key associated with content from result file
     * @param eContent Content from result file
     * @param eSchema Content from schema file
     * @return {TestValue} The new result.
     * @throws ValidationException If validation error occur
     */
    private void ProcessThreshold (TestGroup parent, String key, JsonElement eContent, JsonElement eSchema, MapJsonFailures failures) throws ValidationException {

        if (eContent.isJsonObject()) {
            JsonObject oContent = eContent.getAsJsonObject();

            if (eSchema.isJsonObject()) {
                JsonObject oSchema = eSchema.getAsJsonObject();

                MapJsonThreshold content = new MapJsonThreshold(parent, key, oContent, oSchema);
                Threshold threshold = content.getThreshold();
                if (threshold != null) {
                    parent.addThreshold(threshold);
                    checkThresholdType(threshold);
                }
            }
        }
    }

    /**
     * Process self contained result
     * @param parent Pointer to parent group
     * @param key Key associated with content from result file
     * @param eContent Content from result file
     * @param eSchema Content from schema file
     * @param failures List of failure criteria
     * @throws ValidationException If validation error occur
     */
    private void ProcessParameterFull (TestGroup parent, String key, JsonElement eContent, JsonElement eSchema, MapJsonFailures failures) throws ValidationException {

        MapJsonParameterFull content = new MapJsonParameterFull(key, eContent);
        TestValue parameter = content.get(parent);
        if (parameter != null) {
            parent.addGroup(parameter);
            groups.put(parameter.getGroupHash(), parameter);
            parameters.put(parameter.getGroupHash(), parameter);
        }
    }

    /**
     *  Read the schema for a parameter and load its related content.
     *
     * @param parent Pointer to parent group
     * @param key Key associated with content from result file
     * @param eContent Content from result file
     * @param eSchema Content from schema file
     * @return {TestValue} The new result.
     * @throws ValidationException If validation error occur
     */
    private void ProcessParameter (TestGroup parent, String key, JsonElement eContent, JsonElement eSchema, MapJsonFailures failures) throws ValidationException {

        if (eContent.isJsonObject()) {
            JsonObject oContent = eContent.getAsJsonObject();

            if (eSchema.isJsonObject()) {
                JsonObject oSchema = eSchema.getAsJsonObject();

                MapJsonParameter content = new MapJsonParameter(parent, key, oContent, oSchema, failures, truncateStrings);
                TestValue parameter = content.getParameter();
                if (parameter != null) {
                    parent.addGroup(parameter);
                    groups.put(parameter.getGroupHash(), parameter);
                    if (parameter.getClassType() == TestGroup.ClassType.ct_parameter) {
                        parameters.put(parameter.getGroupHash(), parameter);
                    }
                }
            }
        }
    }

    /**
     * Process self contained result
     *
     * @param parent Pointer to parent group
     * @param key Key associated with content from result file
     * @param eContent Content from result file
     * @param eSchema Content from schema file
     * @param failures List of failure criteria
     * @throws ValidationException If validation error occur
     */
    private void ProcessResultFull (TestGroup parent, String key, JsonElement eContent, JsonElement eSchema, MapJsonFailures failures, boolean booleankey) throws ValidationException {

        TestValue result = null;
        if (booleankey) {
            StringValue res = new StringValue(parent, null, key);
            res.setValue(key);
            res.setFailedState(failures.isFailure(key, true));
            result = (TestValue) res;
        } else {
            MapJsonResultFull content = new MapJsonResultFull(key, eContent);
            result = content.get(parent, failures);
        }
        if (result != null) {
            parent.addGroup(result);
            checkResult(result);

            groups.put(result.getGroupHash(), result);
            results.put(result.getGroupHash(), result);
        }
    }

    /**
     *  Read the schema for a result and load its related content.
     *
     * @param parent Pointer to parent group
     * @param key Key associated with content from result file
     * @param eContent Content from result file
     * @param eSchema Content from schema file
     * @return {TestValue} The new result.
     * @throws ValidationException If validation error occur
     */
    private void ProcessResult (TestGroup parent, String key, JsonElement eContent, JsonElement eSchema, MapJsonFailures failures) throws ValidationException {

        if (eContent.isJsonObject()) {
            JsonObject oContent = eContent.getAsJsonObject();

            if (eSchema.isJsonObject()) {
                JsonObject oSchema = eSchema.getAsJsonObject();

                MapJsonResult content = new MapJsonResult(parent, key, oContent, oSchema, failures, truncateStrings);
                TestValue result = content.getResult();
                if (result != null) {
                    parent.addGroup(result);
                    checkResult(result);

                    groups.put(result.getGroupHash(), result);
                    results.put(result.getGroupHash(), result);

                    // Isolate the other objects and arrays
                    for (Map.Entry<String, JsonElement> enSchema : oSchema.entrySet()) {
                        String kSchema = enSchema.getKey();
                        for (Map.Entry<String, JsonElement> enContent : oContent.entrySet()) {
                            if (kSchema.equals(enContent.getKey())) {
                                ProcessBlock(result, kSchema, enContent.getValue(), enSchema.getValue(), failures);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     *  Read the schema for an object and load its related content.
     *
     * @param parent Pointer to parent group
     * @param key Key associated with content from result file
     * @param eContent Content from result file
     * @param eSchema Content from schema file
     * @return {TestGroup} The new group associated with the object.
     * @throws ValidationException If validation error occur
     */
    private void ProcessObject (TestGroup parent, String key, JsonElement eContent, JsonElement eSchema, MapJsonFailures failures) throws ValidationException {

        if (eContent.isJsonObject()) {
            JsonObject oContent = eContent.getAsJsonObject();

            if (eSchema.isJsonObject()) {
                JsonObject oSchema = eSchema.getAsJsonObject();

                MapJsonGroup content = new MapJsonGroup(parent, key, oContent, oSchema, failures, truncateStrings);
                TestGroup group = content.getGroup();
                if (group != null) {
                    parent.addGroup(group);
                    groups.put(group.getGroupHash(), group);

                    // Isolate the other objects and arrays
                    for (Map.Entry<String, JsonElement> enSchema : oSchema.entrySet()) {
                        String kSchema = enSchema.getKey();
                        for (Map.Entry<String, JsonElement> enContent : oContent.entrySet()) {
                            if (kSchema.equals(enContent.getKey())) {
                                ProcessBlock(group, kSchema, enContent.getValue(), enSchema.getValue(), failures);
                            }
                        }
                    }

                    // Detect if array of parameters
                    group.isParameterGrp();

                    // Detect if array of thresholds
                    group.isThresholdGrp();
                }
            }
        }
    }

    /**
     * Read the schema for an array and load its related content.
     *
     * @param parent Pointer to parent group
     * @param key Key associated with content from result file
     * @param eContent Content from result file
     * @param eSchema Content from schema file
     * @return {TestGroup} The new group associated with the object.
     * @throws ValidationException If validation error occur
     */
    private void ProcessArray(TestGroup parent, String key, JsonElement eContent, JsonElement eSchema, MapJsonFailures failures) throws ValidationException {

        if (eContent.isJsonArray()) {
            JsonArray aContent = eContent.getAsJsonArray();
            if (eSchema.isJsonObject()) {

                TestGroup group = new TestGroup(parent, key, "Array", true);
                if (group != null) {
                    parent.addGroup(group);
                    groups.put(group.getGroupHash(), group);

                    // Go through the content of the array
                    int index = 0;
                    for (JsonElement enContent : aContent) {
                        String kSchema = Integer.toString(index);
                        if (enContent.isJsonObject()) {
                            ProcessBlock(group, kSchema, enContent.getAsJsonObject(), eSchema, failures);
                        }
                        index += 1;
                    }

                    // Detect if array of parameters
                    group.isParameterGrp();

                    // Detect if array of thresholds
                    group.isThresholdGrp();
                }
            }
        }
    }

    /**
     * Convert FilePath to String
     * @param path Original file path.
     * @return String equivalent relative to the workspace.
     */
    private String FilePathToString(FilePath path){
        String relativePath = null;
        String nextChunk = null;
        while(path != null && !path.getName().equalsIgnoreCase("workspace")) {
            if (relativePath == null) {
                if (nextChunk != null) {
                    relativePath = nextChunk;
                }
            } else {
                relativePath = nextChunk + "/" + relativePath;
            }
            nextChunk = path.getName();
            path = path.getParent();
        }
        if (nextChunk != null) {
            relativePath = nextChunk + "/" + relativePath;
        }
        return relativePath;
    }

    /**
     * Retrieve the type of Group tag associate to 'type'
     * @param oSchema Schema Json object
     * @return Enum value for Group tag
     */
    private GroupTags getGroupTag(JsonObject oSchema){
        String type = "";
        for (Map.Entry<String, JsonElement> entrySchema :oSchema.entrySet()) {
            if (entrySchema.getKey().equals("type")) {
                JsonPrimitive primitive = entrySchema.getValue().getAsJsonPrimitive();
                if (primitive.isString()) {
                    type = primitive.getAsString();
                    break;
                }
            }
        }
        type = type.toLowerCase();
        if (type.equals("object")) {
            return GroupTags.gt_object;
        } else if (type.equals("array")) {
            return GroupTags.gt_array;
        } else if (type.equals("resultfull")) {
            return GroupTags.gt_result;
        } else if (type.equals("result")) {
            return GroupTags.gt_result;
        } else if (type.equals("booleankey")) {
            return GroupTags.gt_booleankey;
        } else if (type.equals("threshold")) {
            return GroupTags.gt_threshold;
        } else if (type.equals("parameterfull")) {
            return GroupTags.gt_parameter;
        } else if (type.equals("parameter")) {
            return GroupTags.gt_parameter;
        } else {
            return GroupTags.gt_unknown;
        }
    }
}
