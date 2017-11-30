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
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jenkinsci.plugins.benchmark.results.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Map Result JSON schema/content data to Jenkins plugin data construct
 *
 * @author Daniel Mercier
 * @since 6/20/2017.
 */
public class MapJsonResult {

    /**
     * Enumeration for Group tags
     */
    private enum ResultTags {
        rt_unknown,
        rt_id,
        rt_name,
        rt_description,
        rt_unit,
        rt_message,
        rt_boolean,
        rt_booleankey,
        rt_integer,
        rt_double,
        rt_value
    }

    // Variables
    
    private Integer             id = null;
    private String              name = null;
    private String              description = null;
    private String              unit = null;
    private Map<String, String> messages = new HashMap<String, String>();

    private TestValue result;

    // Constructor

    MapJsonResult(TestGroup parent, String key, JsonObject oContent, JsonObject oSchema, MapJsonFailures failures, boolean truncateStrings){
        ResultTags type = ResultTags.rt_unknown;

        for (Map.Entry<String, JsonElement> enSchema : oSchema.entrySet()) {
            String schemaKey = enSchema.getKey();

            JsonElement schemaValue = enSchema.getValue();
            if (schemaValue.isJsonObject()) {
                JsonObject iSchemaObj = schemaValue.getAsJsonObject();

                type = getResultTag(iSchemaObj);

                switch (type) {
                    case rt_id:
                        if (id == null) {
                            for (Map.Entry<String, JsonElement> enContent : oContent.entrySet()) {
                                if (schemaKey.equals(enContent.getKey())) {
                                    JsonElement value = enContent.getValue();
                                    if (value.isJsonPrimitive()) {
                                        JsonPrimitive primitive = value.getAsJsonPrimitive();
                                        if (primitive.isNumber()) {
                                            id = primitive.getAsInt();
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        break;
                        
                    case rt_name:
                        if (name == null) {
                            for (Map.Entry<String, JsonElement> enContent : oContent.entrySet()) {
                                if (schemaKey.equals(enContent.getKey())) {
                                    JsonElement value = enContent.getValue();
                                    if (value.isJsonPrimitive()) {
                                        JsonPrimitive primitive = value.getAsJsonPrimitive();
                                        if (primitive.isString()) {
                                            name = primitive.getAsString();
                                            name = name.replaceAll(" ", "_");
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        break;

                    case rt_description:
                        if (description == null) {
                            for (Map.Entry<String, JsonElement> enContent : oContent.entrySet()) {
                                if (schemaKey.equals(enContent.getKey())) {
                                    JsonElement value = enContent.getValue();
                                    if (value.isJsonPrimitive()) {
                                        JsonPrimitive primitive = value.getAsJsonPrimitive();
                                        if (primitive.isString()) {
                                            description = primitive.getAsString();
                                            if (truncateStrings && description.length() > 512) {
                                                description = description.substring(0, 512);
                                                description += "...";
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        break;

                    case rt_unit:
                        if (unit == null) {
                            for (Map.Entry<String, JsonElement> enContent : oContent.entrySet()) {
                                if (schemaKey.equals(enContent.getKey())) {
                                    JsonElement value = enContent.getValue();
                                    if (value.isJsonPrimitive()) {
                                        JsonPrimitive primitive = value.getAsJsonPrimitive();
                                        if (primitive.isString()) {
                                            unit = primitive.getAsString();
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        break;

                    case rt_message:
                            for (Map.Entry<String, JsonElement> enContent : oContent.entrySet()) {
                                if (schemaKey.equals(enContent.getKey())) {
                                    JsonElement value = enContent.getValue();
                                    if (value.isJsonPrimitive()) {
                                        JsonPrimitive primitive = value.getAsJsonPrimitive();
                                        if (primitive.isString()) {
                                            if (truncateStrings) {
                                                String message = primitive.getAsString();
                                                if (message.length() > 512) {
                                                    message = message.substring(0, 512);
                                                    message += "...";
                                                }
                                                messages.put(schemaKey, message);
                                            } else {
                                                messages.put(schemaKey,primitive.getAsString());
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        break;
                }
            }
        }

        boolean resultDetected = false;
        for (Map.Entry<String, JsonElement> enSchema : oSchema.entrySet()) {
            String schemaKey = enSchema.getKey();

            JsonElement schemaValue = enSchema.getValue();
            if (schemaValue.isJsonObject()) {
                JsonObject iSchemaObj = schemaValue.getAsJsonObject();

                type = getResultTag(iSchemaObj);
                
                switch (type) {
                    case rt_double:
                        for (Map.Entry<String, JsonElement> enContent : oContent.entrySet()) {
                            if (schemaKey.equals(enContent.getKey())) {
                                if (name == null){
                                    if(key == null) {
                                        name = schemaKey;
                                    } else {
                                        name = key;
                                    }
                                }
                                JsonElement value = enContent.getValue();
                                if (value.isJsonPrimitive()) {
                                    JsonPrimitive primitive = value.getAsJsonPrimitive();
                                    if (primitive.isNumber()) {
                                        DoubleValue res = new DoubleValue(parent, null, name, description, unit);
                                        res.setValue(primitive.getAsDouble());
                                        result = res;
                                        resultDetected = true;
                                        break;
                                    }
                                }
                            }
                        }
                        break;

                    case rt_integer:
                        for (Map.Entry<String, JsonElement> enContent : oContent.entrySet()) {
                            if (schemaKey.equals(enContent.getKey())) {
                                if (name == null){
                                    if(key == null) {
                                        name = schemaKey;
                                    } else {
                                        name = key;
                                    }
                                }
                                JsonElement value = enContent.getValue();
                                if (value.isJsonPrimitive()) {
                                    JsonPrimitive primitive = value.getAsJsonPrimitive();
                                    if (primitive.isNumber()) {
                                        IntegerValue res = new IntegerValue(parent, null, name, description, unit);
                                        res.setValue(primitive.getAsInt());
                                        result = res;
                                        resultDetected = true;
                                        break;
                                    }
                                }
                            }
                        }
                        break;

                    case rt_boolean:
                        for (Map.Entry<String, JsonElement> enContent : oContent.entrySet()) {
                            if (schemaKey.equals(enContent.getKey())) {
                                if (name == null){
                                    if(key == null) {
                                        name = schemaKey;
                                    } else {
                                        name = key;
                                    }
                                }
                                JsonElement value = enContent.getValue();
                                if (value.isJsonPrimitive()) {
                                    JsonPrimitive primitive = value.getAsJsonPrimitive();
                                    if (primitive.isBoolean()) {
                                        BooleanValue res = new BooleanValue(parent, null, name, description, unit);
                                        res.setValue(primitive.getAsBoolean());
                                        if(failures.hasFailures()) {
                                            res.setFailedState(failures.isFailure(primitive.getAsBoolean()));
                                        }
                                        result = res;
                                        resultDetected = true;
                                    } else if(primitive.isString()) {
                                        StringValue res = new StringValue(parent, null, name, description, unit);
                                        res.setValue(primitive.getAsString());
                                        if (failures.hasFailures()) {
                                            res.setFailedState(failures.isFailure(primitive.getAsString()));
                                        }
                                        result = res;
                                        resultDetected = true;
                                    } else if(primitive.isNumber()) {
                                        double dblValue = primitive.getAsDouble();
                                        DoubleValue res = new DoubleValue(parent, null, name, description, unit);
                                        res.setValue(dblValue);
                                        if(failures.hasFailures()) {
                                            res.setFailedState(failures.isFailure(primitive.getAsNumber()));
                                        }
                                        result = res;
                                        resultDetected = true;
                                    }
                                    break;
                                }
                            }
                        }
                        break;

                    case rt_booleankey:
                        for (Map.Entry<String, JsonElement> enContent : oContent.entrySet()) {
                            if (schemaKey.equals(enContent.getKey())) {
                                if (name == null){
                                    if(key == null) {
                                        name = schemaKey;
                                        key = schemaKey;
                                    } else {
                                        name = key;
                                    }
                                }
                                StringValue res = new StringValue(parent, name, description, unit);
                                res.setValue(key);
                                if (failures.hasFailures()) {
                                    res.setFailedState(failures.isFailure(key, true));
                                }
                                result = res;
                                resultDetected = true;
                            }
                        }
                        break;

                    case rt_value:
                        for (Map.Entry<String, JsonElement> enContent : oContent.entrySet()) {
                            if (schemaKey.equals(enContent.getKey())) {
                                if (name == null){
                                    if(key == null) {
                                        name = schemaKey;
                                    } else {
                                        name = key;
                                    }
                                }
                                JsonElement value = enContent.getValue();
                                if (value.isJsonPrimitive()) {
                                    JsonPrimitive primitive = value.getAsJsonPrimitive();
                                    if (primitive.isBoolean()) {
                                        BooleanValue res = new BooleanValue(parent, null, name, description, unit);
                                        res.setValue(primitive.getAsBoolean());
                                        if(failures.hasFailures()) {
                                            res.setFailedState(failures.isFailure(primitive.getAsBoolean()));
                                        }
                                        result = res;
                                        resultDetected = true;
                                    } else if(primitive.isString()) {
                                        StringValue res = new StringValue(parent, null, name, description, unit);
                                        res.setValue(primitive.getAsString());
                                        if(failures.hasFailures()) {
                                            res.setFailedState(failures.isFailure(primitive.getAsString()));
                                        }
                                        result = res;
                                        resultDetected = true;
                                    } else if(primitive.isNumber()) {
                                        double dblValue = primitive.getAsDouble();
                                        DoubleValue res = new DoubleValue(parent, null, name, description, unit);
                                        res.setValue(dblValue);
                                        result = res;
                                        resultDetected = true;
                                    }
                                    break;
                                }
                            }
                        }
                        break;
                }
                if (resultDetected)
                    break;
            }
        }

        if (resultDetected)
            result.setMessages(messages);
    }

    // Functions

    /**
     * Retrieve the type of Result tag associate to 'type'
     * @param {JsonObject} schema
     * @return {ResultTags} Enum value for Result tag
     */
    private ResultTags getResultTag(JsonObject schemaObj) {
        String type = "";
        for (Map.Entry<String, JsonElement> entrySchema : schemaObj.entrySet()) {
            if (entrySchema.getKey().equals("type")) {
                JsonPrimitive primitive = entrySchema.getValue().getAsJsonPrimitive();
                if (primitive.isString()) {
                    type = primitive.getAsString();
                    break;
                }
            }
        }
        type = type.toLowerCase();
        if (type.equals("name")) {
            return ResultTags.rt_name;
        } else if (type.equals("id")) {
            return ResultTags.rt_id;
        } else if (type.equals("description")) {
            return ResultTags.rt_description;
        } else if (type.equals("unit")) {
            return ResultTags.rt_unit;
        } else if (type.equals("message")) {
            return ResultTags.rt_message;
        } else if (type.equals("boolean")) {
            return ResultTags.rt_boolean;
        } else if (type.equals("booleankey")) {
            return ResultTags.rt_booleankey;
        } else if (type.equals("integer")) {
            return ResultTags.rt_integer;
        } else if (type.equals("double")) {
            return ResultTags.rt_double;
        } else if (type.equals("value")) {
            return ResultTags.rt_value;
        } else {
            return ResultTags.rt_unknown;
        }
    }

    // Getter

    public TestValue getResult() { return result; }
}
