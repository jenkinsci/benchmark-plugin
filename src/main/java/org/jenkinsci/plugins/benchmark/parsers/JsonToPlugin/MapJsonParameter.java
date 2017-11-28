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
 * Map Parameter JSON schema/content data to Jenkins plugin data construct
 *
 * @author Daniel Mercier
 * @since 6/20/2017.
 */
public class MapJsonParameter {

    /**
     * Enumeration for Group tags
     */
    private enum ParameterTags {
        pt_unknown,
        pt_id,
        pt_name,
        pt_description,
        pt_unit,
        pt_value,
        pt_message
    }

    // Variables

    private Integer id = null;
    private String  name = null;
    private String  description = null;
    private String  unit = null;
    private Map<String, String> messages = new HashMap<String, String>();

    private TestValue parameter;

    // Constructor

    MapJsonParameter(TestGroup parent, String key, JsonObject oContent, JsonObject oSchema, MapJsonFailures failures, boolean truncateStrings){
        ParameterTags type = ParameterTags.pt_unknown;

        for (Map.Entry<String, JsonElement> enSchema : oSchema.entrySet()) {
            String schemaKey = enSchema.getKey();

            JsonElement schemaValue = enSchema.getValue();
            if (schemaValue.isJsonObject()) {
                JsonObject iSchemaObj = schemaValue.getAsJsonObject();

                type = getParameterTag(iSchemaObj);

                switch (type) {
                    case pt_id:
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

                    case pt_name:
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

                    case pt_description:
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

                    case pt_unit:
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

                    case pt_message:
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

        boolean parameterDetected = false;
        for (Map.Entry<String, JsonElement> enSchema : oSchema.entrySet()) {
            String schemaKey = enSchema.getKey();

            JsonElement schemaValue = enSchema.getValue();
            if (schemaValue.isJsonObject()) {
                JsonObject iSchemaObj = schemaValue.getAsJsonObject();

                type = getParameterTag(iSchemaObj);

                if (type == ParameterTags.pt_value)
                {
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
                                    BooleanValue res = new BooleanValue(parent, null, name, description, unit, TestValue.ClassType.ct_parameter);
                                    res.setValue(primitive.getAsBoolean());
                                    parameter = res;
                                    parameterDetected = true;
                                    break;
                                }  else if (primitive.isString()){
                                    StringValue res = new StringValue(parent, null, name, description, unit, TestValue.ClassType.ct_parameter);
                                    res.setValue(primitive.getAsString());
                                    parameter = res;
                                    parameterDetected = true;
                                    break;
                                } else if (primitive.isNumber()) {
                                    DoubleValue res = new DoubleValue(parent, null, name, description, unit, TestValue.ClassType.ct_parameter);
                                    res.setValue(primitive.getAsDouble());
                                    parameter = res;
                                    parameterDetected = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (parameterDetected)
                    break;
            }
        }
        if (parameterDetected) {
            parameter.setMessages(messages);
        }
    }

    // Functions

    /**
     * Retrieve the type of Parameter tag associate to 'type'
     * @param schemaObj Schema Json object
     * @return Enum value for Parameter tag
     */
    private ParameterTags getParameterTag(JsonObject schemaObj){
        String type = "";
        for (Map.Entry<String, JsonElement> entrySchema :schemaObj.entrySet()) {
            if (entrySchema.getKey().equals("type")) {
                JsonPrimitive primitive = entrySchema.getValue().getAsJsonPrimitive();
                if (primitive.isString()) {
                    type = primitive.getAsString();
                    break;
                }
            }
        }
        type = type.toLowerCase();

        if (type.equals("id")) {
            return ParameterTags.pt_id;
        } else if (type.equals("name")) {
            return ParameterTags.pt_name;
        } else if (type.equals("description")) {
            return ParameterTags.pt_description;
        } else if (type.equals("unit")) {
            return ParameterTags.pt_unit;
        } else if (type.equals("value")) {
            return ParameterTags.pt_value;
        } else if (type.equals("message")) {
            return ParameterTags.pt_message;
        } else {
            return ParameterTags.pt_unknown;
        }
    }

    // Getter

    public TestValue getParameter() { return parameter; }
}

