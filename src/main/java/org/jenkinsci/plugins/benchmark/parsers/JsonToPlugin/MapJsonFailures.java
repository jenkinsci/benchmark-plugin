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


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jenkinsci.plugins.benchmark.exceptions.ValidationException;
import org.jenkinsci.plugins.benchmark.results.TestFailure;
import org.jenkinsci.plugins.benchmark.results.TestGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Map Failure JSON schema data to Jenkins plugin data construct
 *
 * @author Daniel Mercier
 * @since 6/20/2017.
 */
public class MapJsonFailures {

    // Variables

    private List<TestFailure> failures;

    // Constructor

    MapJsonFailures(TestGroup parent, JsonElement eSchema) throws ValidationException {
        failures = new ArrayList<TestFailure>();
        LoadFailures(parent, eSchema);
    }

    MapJsonFailures(TestGroup parent, JsonElement eSchema, MapJsonFailures oFailures) throws ValidationException{
        failures = new ArrayList<TestFailure>();
        LoadFailures(parent, eSchema);
        if (oFailures != null) {
            failures.addAll(oFailures.getFailures());
        }
    }

    // Functions

    /**
     * Load all the failures detected inside the passed JsonElement
     * @param eSchema JsonElement containing the schema
     */
    private void LoadFailures(TestGroup parent, JsonElement eSchema) throws ValidationException {
        if (eSchema.isJsonObject()) {
            JsonObject oSchema = eSchema.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entrySchema : oSchema.entrySet()) {
                if (entrySchema.getKey().equals("failures")) {
                    JsonElement eFailure = entrySchema.getValue();
                    if (eFailure.isJsonArray()) {
                        JsonArray aFailure = eFailure.getAsJsonArray();
                        for (JsonElement entryFailure : aFailure) {
                            if (entryFailure.isJsonObject()) {
                                LoadFailureObject(parent, entryFailure);
                            }
                        }
                    }
                }
                if (entrySchema.getKey().equals("failure")) {
                    JsonElement eFailure = entrySchema.getValue();

                    // Process primitives
                    if (eFailure.isJsonPrimitive()) {
                        JsonPrimitive pFailure = eFailure.getAsJsonPrimitive();
                        if (pFailure.isString()) {
                            failures.add(new TestFailure(pFailure.getAsString()));
                        } else if (pFailure.isNumber()) {
                            failures.add(new TestFailure(pFailure.getAsDouble()));
                        }else if (pFailure.isBoolean()) {
                            failures.add(new TestFailure(pFailure.getAsBoolean()));
                        }
                    }

                    // Process object
                    if (eFailure.isJsonObject()) {
                        LoadFailureObject(parent, eFailure);
                    }
                }
            }
        }
    }

    /**
     * Load the content of single failure object
     * @param eFailure JsonElement containing the failure object
     */
    private void  LoadFailureObject(TestGroup parent, JsonElement eFailure) throws ValidationException {
        JsonObject oFailure = eFailure.getAsJsonObject();

        String compareType = null;
        for (Map.Entry<String, JsonElement> entryFailure : oFailure.entrySet()) {
            if (entryFailure.getKey().equals("compare")) {
                JsonElement eType = entryFailure.getValue();
                if (eType.isJsonPrimitive()) {
                    JsonPrimitive pType = eType.getAsJsonPrimitive();
                    if (pType.isString()) {
                        compareType = pType.getAsString();
                    }
                }
            }
        }
        for (Map.Entry<String, JsonElement> entryFailure : oFailure.entrySet()) {
            if (entryFailure.getKey().equals("value")) {
                JsonElement eType = entryFailure.getValue();
                if (eType.isJsonPrimitive()) {
                    JsonPrimitive pType = eType.getAsJsonPrimitive();
                    if (pType.isString()){
                        failures.add(new TestFailure(pType.getAsString()));
                    } else if (pType.isBoolean()) {
                        failures.add(new TestFailure(pType.getAsBoolean()));
                    } else if (pType.isNumber()) {
                        try{
                            failures.add(new TestFailure(pType.getAsDouble(), compareType));
                        } catch (Exception e) {
                            throw new ValidationException(Messages.MapJsonFailures_CompareIsNotRecognisedAsType(compareType, parent.getName()));
                        }
                    }
                }
            }
            if (entryFailure.getKey().equals("key")) {
                JsonElement eType = entryFailure.getValue();
                if (eType.isJsonPrimitive()) {
                    JsonPrimitive pType = eType.getAsJsonPrimitive();
                    if (pType.isString())
                        failures.add(new TestFailure(pType.getAsString(), true));
                }
            }
        }
    }

    public Boolean isFailure(boolean value) {
        for (TestFailure failure : failures) {
            if (failure.isFailure(value))
                return true;
        }
        return false;
    }

    public Boolean isFailure(Number value){
        for (TestFailure failure : failures) {
            if (failure.isFailure(value.doubleValue()))
                return true;
        }
        return false;
    }

    public Boolean isFailure(String value){
        for (TestFailure failure : failures) {
            if (failure.isFailure(value))
                return true;
        }
        return false;
    }

    public Boolean isFailure(String value, boolean key){
        for (TestFailure failure : failures) {
            if (failure.isFailure(value, key))
                return true;
        }
        return false;
    }

    // Getter

    public int size(){ return failures.size(); }
    public List<TestFailure> getFailures(){ return failures; }
    public boolean hasFailures() { return (failures.size() > 0); }
}
