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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jenkinsci.plugins.benchmark.exceptions.ValidationException;
import org.jenkinsci.plugins.benchmark.thresholds.Threshold;

import java.io.InvalidClassException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Math.sqrt;

/**
 * Holds the information for double test result
 *
 * @author Daniel Mercier
 * @since 5/10/2017
 */
public class DoubleValue extends NumeralValue {

    // Variables

    protected final ConcurrentHashMap<Integer, Double> values;

    // Constructor

    public DoubleValue(TestGroup parent, String group, String name) {
        super(parent, group, name, null, null, ValueType.rt_double);
        this.values = new ConcurrentHashMap<Integer, Double>();
    }

    public DoubleValue(TestGroup parent, String group, String name, String unit) {
        super(parent, group, name, null, unit, ValueType.rt_double);
        this.values = new ConcurrentHashMap<Integer, Double>();
    }

    public DoubleValue(TestGroup parent, String group, String name, String description, String unit) {
        super(parent, group, name, description, unit, ValueType.rt_double);
        this.values = new ConcurrentHashMap<Integer, Double>();
    }

    public DoubleValue(TestGroup parent, String group, String name, ClassType ctype) {
        super(parent, group, name, null, null, ValueType.rt_double, ctype);
        this.values = new ConcurrentHashMap<Integer, Double>();
    }

    public DoubleValue(TestGroup parent, String group, String name, String unit, ClassType ctype) {
        super(parent, group, name, null, unit, ValueType.rt_double, ctype);
        this.values = new ConcurrentHashMap<Integer, Double>();
    }

    public DoubleValue(TestGroup parent, String group, String name, String description, String unit, ClassType ctype) {
        super(parent, group, name, description, unit, ValueType.rt_double, ctype);
        this.values = new ConcurrentHashMap<Integer, Double>();
    }

    // Functions

    /**
     * Get previous build value
     * @param build Build number
     * @return previous
     */
    public Double getPreviousValue(int build) {
        while (build > 0){
            Double value = this.values.get(build);
            if (value != null) {
                return value;
            }
            build--;
        }
        return null;
    }

    /**
     * Calculate average
     * @return average
     */
    public Double calculateAverage() {
        int number = 0;
        double sum = 0.0;
        for (Map.Entry<Integer, Double> entry : this.values.entrySet()) {
            TestProperty property = this.properties.get(entry.getKey());
            if (property == null) {
                sum += entry.getValue();
                number++;
            } else {
                Boolean failedState = property.getFailedState();
                if (failedState == null || failedState == false) {
                    sum += entry.getValue();
                    number++;
                }
            }
        }
        if (number == 0) {
            return null;
        } else {
            return sum / number;
        }
    }

    /**
     * Create an JSON object with the condensed information of this result [EXPORT CONDENSED]
     * @param build Build Number
     * @param hash Result hash
     * @return JSON object
     */
    @Override
    public JsonObject getCondensedJsonObject (int build, int hash) {

        int failed = 0;
        int passed = 0;

        Double minimum = null;
        Double maximum = null;
        Double std_deviation = null;

        // Calculate condensed values
        Double average = calculateAverage();
        if (average != null){
            int number = 0;
            std_deviation = 0.0;
            for (Map.Entry<Integer, Double> entry : this.values.entrySet()) {
                Boolean failedState;
                TestProperty property = this.properties.get(entry.getKey());
                if (property == null) {
                    failedState = null;
                } else {
                    failedState = property.getFailedState();
                }
                if (failedState != null) {
                    if (failedState) {
                        failed++;
                    } else {
                        passed++;
                    }
                }
                if (failedState == null || failedState == false) {
                    Double value = entry.getValue();
                    if (number == 0){
                        minimum = value;
                        maximum = value;
                    } else {
                        if (value < minimum) {
                            minimum = value;
                        } else if (value > maximum){
                            maximum = value;
                        }
                    }
                    std_deviation += (value - average)*(value - average);
                    number++;
                }
            }
            std_deviation = sqrt(std_deviation/number);
        }

        // Assemble JSON object
        JsonObject object = new JsonObject();
        object.addProperty("hash", hash);
        if (this.getFileGroup() != null) {
            object.addProperty("file", this.getFileGroup().getGroupHash());
        }
        if (this.group != null && !this.group.isEmpty()) {
            object.addProperty("group", this.group);
        }
        object.addProperty("name", this.name);
        if (this.description != null && !this.description.isEmpty()) {
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
        if (average != null){
            object.addProperty("previous", this.getPreviousValue(build));
            object.addProperty("average", average);
            object.addProperty("std_deviation", std_deviation);
            object.addProperty("minimum", minimum);
            object.addProperty("maximum", maximum);
        }
        object.addProperty("failed", failed);
        object.addProperty("passed", passed);
        return object;
    }

    /**
     * Create JSON object containing all results necessary to display the graph [DETAIL PAGE]
     * @param buildNumbers List of builds
     * @return Json object
     * @throws InvalidClassException Invalid class
     */
    @Override
    public JsonArray getDataAsJsonArray(TreeSet<Integer> buildNumbers) throws InvalidClassException {
        JsonArray array = new JsonArray();
        for (Integer build = buildNumbers.last(); build >= buildNumbers.first(); build--) {
            JsonObject object = new JsonObject();
            Double value = this.getValue(build);
            if (value == null) {
                object.addProperty("x", build);
                object.add("y", null);
            } else {
                object.addProperty("x", build);
                object.addProperty("y", value);
            }
            array.add(object);
        }
        return array;
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
        Double value = this.getValue(build);
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
        Double value = this.getValue(build);
        if (value == null) {
            return "";
        } else {
            return value.toString().replace('.', decimalSeparator);
        }
    }

    /**
     * Check attached thresholds to verify result validity.
     * @param previous Previous value
     * @param average Calculated average
     */
    @Override
    public void checkThresholdStatus(Double previous, Double average) {
        List<Threshold> list = getAllConnectedThresholds();
        for (Threshold threshold : list) {
            try {
                threshold.setAverageValue(average);
                threshold.setPreviousValue(previous);
                threshold.isValid(values.get(0));
                setFailedState(false);
            } catch (ValidationException e) {
                setMessage(threshold.getName(), e.getMessage());
                setFailedState(true);
            }
        }
    }

    // Setter

    public void setValue(double value) { this.values.put(0, value); }
    public void setValue(int build, double value) { this.values.put(build, value); }

    // Getter

    public Map<Integer, Double> getValues() { return this.values; }
    public Double getValue() throws NullPointerException { return this.values.get(0); }
    public Double getValue(int build) throws NullPointerException { return this.values.get(build); }


}