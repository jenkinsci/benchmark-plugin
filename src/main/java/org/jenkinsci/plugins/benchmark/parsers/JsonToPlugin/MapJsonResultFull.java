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

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.jenkinsci.plugins.benchmark.results.*;

/**
 * Map Full Result JSON schema/content data to Jenkins plugin data construct
 *
 * @author Daniel Mercier
 * @since 6/20/2017.
 */
public class MapJsonResultFull {

    // Variables

    private final String        key;
    private final JsonElement   content;

    // Constructor

    MapJsonResultFull(String key, JsonElement content){
        this.key = key;
        this.content = content;
    }

    // Functions

    public TestValue get(TestGroup parent, MapJsonFailures failures){
        if (content.isJsonPrimitive()) {
            JsonPrimitive primitive = content.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                BooleanValue result = new BooleanValue(parent, null, key);
                result.setValue(primitive.getAsBoolean());
                if(failures.hasFailures()) {
                    result.setFailedState(failures.isFailure(primitive.getAsBoolean()));
                }
                return (TestValue) result;
            } else if(primitive.isString()) {
                StringValue result = new StringValue(parent, null, key);
                result.setValue(primitive.getAsString());
                if (failures.hasFailures()) {
                    result.setFailedState(failures.isFailure(primitive.getAsString()));
                }
                return (TestValue) result;
            } else if(primitive.isNumber()) {
                DoubleValue result = new DoubleValue(parent, null, key);
                result.setValue(primitive.getAsDouble());
                return (TestValue) result;
            }
        }
        return null;
    }

}
