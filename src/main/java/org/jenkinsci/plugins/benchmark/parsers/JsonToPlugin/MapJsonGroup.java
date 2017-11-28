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
import org.jenkinsci.plugins.benchmark.results.TestGroup;

import java.util.Map;

/**
 * Map Group JSON schema/content data to Jenkins plugin data construct
 *
 * @author Daniel Mercier
 * @since 6/20/2017.
 */
public class MapJsonGroup {

    /**
     * Enumeration for Group tags
     */
    private enum GroupTags {
        gt_unknown,
        gt_name,
        gt_description
    }

    // Variables

    private String name = null;
    private String description = null;

    private TestGroup group = null;

    // Constructor

    MapJsonGroup(TestGroup parent, String key, JsonObject oContent, JsonObject oSchema, MapJsonFailures failures, boolean truncateStrings){
        GroupTags type = GroupTags.gt_unknown;

        for (Map.Entry<String, JsonElement> enSchema : oSchema.entrySet()) {
            String schemaKey = enSchema.getKey();

            JsonElement schemaValue = enSchema.getValue();
            if (schemaValue.isJsonObject()) {
                JsonObject iSchemaObj = schemaValue.getAsJsonObject();

                type = getGroupTag(iSchemaObj);

                switch (type) {
                    case gt_name:
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

                    case gt_description:
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
                }
            }
        }
        if (name == null){
            name = key;
        }
        group = new TestGroup(parent, name, description);
    }

    /**
     * Retrieve the type of Group tag associate to 'type'
     * @param {JsonObject} schema
     * @return {GroupTags} Enum value for Group tag
     */
    private GroupTags getGroupTag(JsonObject schemaObj){
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
        if (type.equals("name"))
            return GroupTags.gt_name;
        else if (type.equals("description"))
            return GroupTags.gt_description;
        else
            return GroupTags.gt_unknown;
    }

    // Getter

    public TestGroup getGroup() { return group; }

}
