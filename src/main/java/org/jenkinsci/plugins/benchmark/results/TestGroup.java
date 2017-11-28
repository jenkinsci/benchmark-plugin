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
package org.jenkinsci.plugins.benchmark.results;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jenkinsci.plugins.benchmark.thresholds.Threshold;
import org.jenkinsci.plugins.benchmark.utilities.ContentDetected;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Holds a group of test results or group of groups
 *
 * @author Daniel Mercier
 * @since 5/10/2017
 */
public class TestGroup {

    public enum ClassType {
        ct_group,
        ct_array,
        ct_result,
        ct_parameter,
        ct_parameterGrp,
        ct_thresholdGrp,
        ct_fileGrp
    }

    // Variables

    protected final TestGroup           parent;
    protected final String              name;
    protected final String              description;
    protected final List<TestGroup>     groups;
    protected final List<Threshold>     thresholds;
    protected final int                 groupHash;
    protected       ClassType           ctype;

    // Constructor

    public TestGroup(TestGroup parent, String name){
        this.parent = parent;
        this.name = name;
        this.description = "";
        this.groups = new ArrayList<TestGroup>();
        this.thresholds = new ArrayList<Threshold>();
        this.groupHash = this.getFullName().hashCode();
        this.ctype = ClassType.ct_group;
    }

    public TestGroup(TestGroup parent, String name, String description){
        this.parent = parent;
        this.name = name;
        if (description == null)
            this.description = "";
        else
            this.description = description;
        this.groups = new ArrayList<TestGroup>();
        this.thresholds = new ArrayList<Threshold>();
        this.groupHash = this.getFullName().hashCode();
        this.ctype = ClassType.ct_group;
    }

    public TestGroup(TestGroup parent, String name, ClassType type){
        this.parent = parent;
        this.name = name;
        this.description = "";
        this.groups = new ArrayList<TestGroup>();
        this.thresholds = new ArrayList<Threshold>();
        this.groupHash = this.getFullName().hashCode();
        this.ctype = type;
    }

    public TestGroup(TestGroup parent, String name, String description, ClassType type){
        this.parent = parent;
        this.name = name;
        if (description == null)
            this.description = "";
        else
            this.description = description;
        this.groups = new ArrayList<TestGroup>();
        this.thresholds = new ArrayList<Threshold>();
        this.groupHash = this.getFullName().hashCode();
        this.ctype = type;
    }

    public TestGroup(TestGroup parent, String name, boolean array){
        this.parent = parent;
        this.name = name;
        this.description = "";
        this.groups = new ArrayList<TestGroup>();
        this.thresholds = new ArrayList<Threshold>();
        this.groupHash = this.getFullName().hashCode();
        this.ctype = ClassType.ct_array;
    }

    public TestGroup(TestGroup parent, String name, String description, boolean array){
        this.parent = parent;
        this.name = name;
        if (description == null)
            this.description = "";
        else
            this.description = description;
        this.groups = new ArrayList<TestGroup>();
        this.thresholds = new ArrayList<Threshold>();
        this.groupHash = this.getFullName().hashCode();
        this.ctype = ClassType.ct_array;
    }

    // Functions

    /**
     * Check for file group in the Jenkins plugin data format, if notne, create one
     * @param object Original object to parse
     * @param rootGroup Point of origin where to attach the data tree
     * @param entityList List of entities grenerated
     * @param detected Key characteristics fo results
     */
    public static void convertCondensedFileJsonObject(JsonObject object, TestGroup rootGroup, Map<Integer, TestGroup> entityList, ContentDetected detected) {
        Integer _hash = null;
        String _name = null;
        String _description = null;

        for (Map.Entry<String, JsonElement> enObject : object.entrySet()) {
            if (enObject.getKey().equalsIgnoreCase("hash")) {
                JsonElement enElement = enObject.getValue();
                if (enElement.isJsonPrimitive()) {
                    JsonPrimitive primitive = enElement.getAsJsonPrimitive();
                    if (primitive.isNumber()) {
                        _hash = primitive.getAsInt();
                    }
                }
                break;
            }
        }
        TestGroup grp = entityList.get(_hash);
        if (grp == null) {
            for (Map.Entry<String, JsonElement> enObject : object.entrySet()) {
                if (enObject.getKey().equalsIgnoreCase("name")) {
                    JsonElement value = enObject.getValue();
                    if (value.isJsonPrimitive()) {
                        JsonPrimitive primitive = value.getAsJsonPrimitive();
                        if (primitive.isString()) {
                            _name = primitive.getAsString();
                        }
                    }
                } else if (enObject.getKey().equalsIgnoreCase("description")) {
                    JsonElement value = enObject.getValue();
                    if (value.isJsonPrimitive()) {
                        JsonPrimitive primitive = value.getAsJsonPrimitive();
                        if (primitive.isString()) {
                            _description = primitive.getAsString();
                        }
                    }
                }
            }
            TestGroup group = new TestGroup(rootGroup, _name, _description, ClassType.ct_fileGrp);
            rootGroup.addGroup(group);
            entityList.put(_hash, group);
            detected.setFileDetected(true);
        }
    }

    public boolean isParameterGrp(){
        int nParameters = 0;
        for (TestGroup group:this.groups){
            if (group.getClassType() == ClassType.ct_parameter || group.getClassType() == ClassType.ct_parameterGrp) {
                nParameters++;
            }
        }
        if (nParameters == this.groups.size() && this.thresholds.size() == 0) {
            this.ctype = ClassType.ct_parameterGrp;
            return true;
        }
        return false;
    }

    public boolean isThresholdGrp(){
        if (this.groups.size() == 0 && this.thresholds.size() != 0){
            this.ctype = ClassType.ct_thresholdGrp;
            return true;
        }
        return false;
    }

    public JsonObject getJsonObject(int hash){
        JsonObject object = new JsonObject();
        if (this.getName() != null){
            object.addProperty("hash", hash);
        }
        if (this.getName() != null){
            object.addProperty("name", this.getName());
        }
        if (this.getDescription() != null){
            object.addProperty("description", this.getDescription());
        }
        return object;
    }

    public void addGroup(TestGroup testGroup) { this.groups.add(testGroup); }
    public  TestGroup getGroup(int index) throws ArrayIndexOutOfBoundsException{
        if(index > this.groups.size())
            throw new ArrayIndexOutOfBoundsException(Messages.TestGroup_SelectedIndexOutOfBound());
        return this.groups.get(index);
    }
    public int getNumberOfGroups() { return this.groups.size(); }

    public List<TestGroup> getTestGroups(){ return this.groups; }

    public List<TestGroup> getConnectedParameters(){
        List<TestGroup> list = new ArrayList<TestGroup>();
        for(TestGroup group:groups){
            if (group.getClassType() == ClassType.ct_parameter) {
                list.add(group);
            } else if (group.getClassType() == ClassType.ct_parameterGrp) {
                list.addAll(group.getConnectedParameters());
            }
        }
        return list;
    }

    public List<TestGroup> getConnectedParentParameters(){
        List<TestGroup> list = new ArrayList<TestGroup>();
        if(parent != null){
            list.addAll(parent.getAllConnectedParameters());
        }
        return list;
    }

    public List<TestGroup> getAllConnectedParameters(){
        List<TestGroup> list = new ArrayList<TestGroup>();
        list.addAll(getConnectedParameters());
        list.addAll(getConnectedParentParameters());
        return list;
    }

    public void addThreshold(Threshold threshold) { this.thresholds.add(threshold); }
    public Threshold getThreshold(int index) throws ArrayIndexOutOfBoundsException{
        if(index > this.thresholds.size())
            throw new ArrayIndexOutOfBoundsException(Messages.TestGroup_SelectedIndexOutOfBound());
        return this.thresholds.get(index);
    }
    public int getNumberOfThresholds() { return this.thresholds.size(); }

    public List<Threshold> getConnectedThresholds(){
        List<Threshold> list = new ArrayList<Threshold>();
        list.addAll(this.thresholds);
        for(TestGroup group:this.groups){
            if (group.getClassType() == ClassType.ct_thresholdGrp){
                list.addAll(group.getConnectedThresholds());
            }
        }
        return list;
    }

    public List<Threshold> getConnectedParentThresholds(){
        List<Threshold> list = new ArrayList<Threshold>();
        if(parent != null){
            list.addAll(parent.getConnectedThresholds());
            list.addAll(parent.getConnectedParentThresholds());
        }
        return list;
    }

    public List<Threshold> getAllConnectedThresholds(){
        List<Threshold> list = new ArrayList<Threshold>();
        list.addAll(getConnectedThresholds());
        list.addAll(getConnectedParentThresholds());
        return list;
    }

    // Getters

    public TestGroup getParent() { return parent; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ClassType getClassType() { return ctype; }
    public Integer getGroupHash() { return groupHash; }
    public Integer getFileHash() {
        if (this.getClassType() == ClassType.ct_fileGrp) {
            return getGroupHash();
        } else {
            if (this.parent == null) {
                return null;
            } else {
                return this.parent.getFileHash();
            }
        }
    }

    public boolean isArray() { return (ctype == ClassType.ct_array); }
    public String getFullName() {
        if (this.name.equalsIgnoreCase("__root__") || this.name.equalsIgnoreCase("__first__")) {
            return "";
        } else {
            String fullName = this.parent.getFullName();
            if (this.getClassType() == ClassType.ct_fileGrp) {
                if (fullName.length() > 0) {
                    return (fullName + '.' + this.description);
                } else {
                    return this.description;
                }
            } else {
                if (fullName.length() > 0) {
                    return (fullName + '.' + this.name);
                } else {
                    return this.name;
                }
            }
        }
    }
    public String getFileSubGroupFullName() {
        if (this.name.equalsIgnoreCase("__root__") || this.name.equalsIgnoreCase("__first__")) {
            return "";
        } else {
            if (parent.getClassType() == ClassType.ct_fileGrp) {
                return this.name;
            } else {
                String fullName = this.parent.getFileSubGroupFullName();
                if (fullName.length() > 0) {
                    return (fullName + '.' + this.name);
                } else {
                    return this.name;
                }
            }
        }
    }

    public List<String> getDescriptions(){
        if (parent == null) {
            List<String> descriptions = new ArrayList<String>();
            descriptions.add(description);
            return descriptions;
        } else {
            List<String> list = parent.getDescriptions();
            list.add(description);
            return list;
        }
    }

    protected TestGroup getFileGroup(){
        if (this.name.equalsIgnoreCase("__root__") || this.name.equalsIgnoreCase("__first__")) {
            return null;
        } else {
            if (this.parent.getClassType() == ClassType.ct_fileGrp){
                return this.parent;
            } else {
                return this.parent.getFileGroup();
            }
        }
    }

}
