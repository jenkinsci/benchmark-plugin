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

import com.google.gson.JsonObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the information for boolean test result
 *
 * @author Daniel Mercier
 * @since 5/10/2017
 */
public class BooleanValue extends TestValue {

    // Variables

    private final ConcurrentHashMap<Integer, Boolean> values;

    // Constructor

    public BooleanValue(TestGroup parent, String group, String name){
        super(parent, group, name, null, null,  ValueType.rt_boolean);
        this.values = new ConcurrentHashMap<Integer, Boolean>();
    }

    public BooleanValue(TestGroup parent, String group, String name, String unit){
        super(parent, group, name, null, unit,  ValueType.rt_boolean);
        this.values = new ConcurrentHashMap<Integer, Boolean>();
    }

    public BooleanValue(TestGroup parent, String group, String name, String description, String unit){
        super(parent, group, name, description, unit, ValueType.rt_boolean);
        this.values = new ConcurrentHashMap<Integer, Boolean>();
    }

    public BooleanValue(TestGroup parent, String group, String name, ClassType ctype){
        super(parent, group, name, null, null, ValueType.rt_boolean, ctype);
        this.values = new ConcurrentHashMap<Integer, Boolean>();
    }

    public BooleanValue(TestGroup parent, String group, String name, String unit, ClassType ctype){
        super(parent, group, name, null, unit, ValueType.rt_boolean, ctype);
        this.values = new ConcurrentHashMap<Integer, Boolean>();
    }

    public BooleanValue(TestGroup parent, String group, String name, String description, String unit, ClassType ctype){
        super(parent, group, name, description, unit, ValueType.rt_boolean, ctype);
        this.values = new ConcurrentHashMap<Integer, Boolean>();
    }

    // Functions

    /**
     * Create an JSON object with the condensed information of this result [EXPORT CONDENSED]
     * @param build Build number
     * @param hash Result hash
     * @return JSON object
     */
    @Override
    public JsonObject getCondensedJsonObject(int build, int hash) {
        int failed = 0;
        int passed = 0;

        // Calculate condensed values
        for (TestProperty property : properties.values()) {
            Boolean failedState = property.getFailedState();
            if (failedState != null) {
                if (failedState) {
                    failed++;
                } else {
                    passed++;
                }
            }
        }

        // Assemble JSON object
        JsonObject object = new JsonObject();
        object.addProperty("hash", hash);
        if(this.group != null && !this.group.isEmpty()) {
            object.addProperty("group", this.group);
        }
        object.addProperty("name", this.name);
        if(this.description != null && !this.description.isEmpty()) {
            object.addProperty("description", this.description);
        }
        if(this.unit != null && !this.unit.isEmpty()) {
            object.addProperty("unit", this.unit);
        }
        object.addProperty("type", outputType(this.type));
        if (this.ctype == ClassType.ct_result) {
            Integer _fileHash = this.getFileHash();
            if (_fileHash != null) {
                object.addProperty("file", _fileHash);
            }
        }
        object.addProperty("failed", failed);
        object.addProperty("passed", passed);
        return object;
    }

    /**
     * Get last build result content in Json Object [EXPORT RAW]
     * Works with TestValue getJsonObject()
     * @param hash Result hash
     * @return Json object
     */
    @Override
    public JsonObject getJsonObject(int hash) {
        JsonObject object = super.getJsonObject(hash);
        object.addProperty("value", this.getValue());
        return object;
    }

    /**
     * Return last value as string in default locale [TABLE PAGE][DETAIL PAGE]
     * @param build Build number
     * @return String of value
     */
    @Override
    public String getValueAsString(int build) {
        Boolean value = this.getValue(build);
        if (value == null) {
            return "";
        } else {
            return value.toString();
        }
    }

    /**
     * Return last value as string in locale format [TABLE PAGE][DETAIL PAGE]
     * @param build Build number
     * @param decimalSeparator Decimal separator
     * @return String of value
     */
    @Override
    public String getValueAsLocaleString(int build, char decimalSeparator) {
        Boolean value = this.getValue(build);
        if (value == null) {
            return "";
        } else {
            return "__boolean__";
        }
    }

    // Setter

    public void setValue( boolean value ){ this.values.put(0, value); }
    public void setValue( int build, boolean value ){ this.values.put(build, value); }

    // Getter

    public Map<Integer, Boolean> getValues() { return this.values; }
    public Boolean getValue() { return this.values.get(0); }
    public Boolean getValue(int build) { return this.values.get(build); }
}
