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
import org.jenkinsci.plugins.benchmark.exceptions.ValidationException;
import org.jenkinsci.plugins.benchmark.results.TestGroup;
import org.jenkinsci.plugins.benchmark.thresholds.*;

import java.util.Map;

/**
 * Map Threshold JSON schema/content data to Jenkins plugin data construct
 *
 * @author Daniel Mercier
 * @since 6/20/2017.
 */
public class MapJsonThreshold {
    /**
     * Enumeration of Threshold tags
     */
    enum ThresholdTags {
        tt_unknown,
        tt_method,
        tt_minimum,
        tt_maximum,
        tt_delta,
        tt_percentage,
        tt_ignoreNegativeDeltas
    }

    // Variables

    private Double minimum;
    private Double maximum;
    private Double delta;
    private Double percentage;
    private Boolean ignoreNegativeDeltas;

    private Threshold threshold;

    // Constructor

    MapJsonThreshold(TestGroup parent, String key, JsonObject oContent, JsonObject oSchema) throws ValidationException {
        ThresholdTags type = ThresholdTags.tt_unknown;

        for (Map.Entry<String, JsonElement> enSchema : oSchema.entrySet()) {
            String schemaKey = enSchema.getKey();

            JsonElement schemaValue = enSchema.getValue();
            if (schemaValue.isJsonObject()) {
                JsonObject iSchemaObj = schemaValue.getAsJsonObject();

                type = getThresholdTag(iSchemaObj);

                switch (type) {
                    case tt_minimum:
                        if (minimum == null) {
                            for (Map.Entry<String, JsonElement> enContent : oContent.entrySet()) {
                                if (schemaKey.equals(enContent.getKey())) {
                                    JsonElement value = enContent.getValue();
                                    if (value.isJsonPrimitive()) {
                                        JsonPrimitive primitive = value.getAsJsonPrimitive();
                                        if (primitive.isNumber()) {
                                            minimum = primitive.getAsDouble();
                                            break;
                                        } else {
                                            throw new ValidationException(Messages.MapJsonThreshold_WrongFormatForMinimum());
                                        }
                                    }
                                }
                            }
                        }
                        break;

                    case tt_maximum:
                        if (maximum == null) {
                            for (Map.Entry<String, JsonElement> enContent : oContent.entrySet()) {
                                if (schemaKey.equals(enContent.getKey())) {
                                    JsonElement value = enContent.getValue();
                                    if (value.isJsonPrimitive()) {
                                        JsonPrimitive primitive = value.getAsJsonPrimitive();
                                        if (primitive.isNumber()) {
                                            maximum = primitive.getAsDouble();
                                            break;
                                        } else {
                                            throw new ValidationException(Messages.MapJsonThreshold_WrongFormatForMaximum());
                                        }
                                    }
                                }
                            }
                        }
                        break;

                    case tt_delta:
                        if (delta == null) {
                            for (Map.Entry<String, JsonElement> enContent : oContent.entrySet()) {
                                if (schemaKey.equals(enContent.getKey())) {
                                    JsonElement value = enContent.getValue();
                                    if (value.isJsonPrimitive()) {
                                        JsonPrimitive primitive = value.getAsJsonPrimitive();
                                        if (primitive.isNumber()) {
                                            delta = primitive.getAsDouble();
                                            break;
                                        } else {
                                            throw new ValidationException(Messages.MapJsonThreshold_WrongFormatForDelta());
                                        }
                                    }
                                }
                            }
                        }
                        break;

                    case tt_percentage:
                        if (percentage == null) {
                            for (Map.Entry<String, JsonElement> enContent : oContent.entrySet()) {
                                if (schemaKey.equals(enContent.getKey())) {
                                    JsonElement value = enContent.getValue();
                                    if (value.isJsonPrimitive()) {
                                        JsonPrimitive primitive = value.getAsJsonPrimitive();
                                        if (primitive.isNumber()) {
                                            percentage = primitive.getAsDouble();
                                            break;
                                        } else {
                                            throw new ValidationException(Messages.MapJsonThreshold_WrongFormatForPercentage());
                                        }
                                    }
                                }
                            }
                        }
                        break;

                    case tt_ignoreNegativeDeltas:
                        if (ignoreNegativeDeltas == null){
                            for (Map.Entry<String, JsonElement> enContent : oContent.entrySet()) {
                                if (schemaKey.equals(enContent.getKey())) {
                                    JsonElement value = enContent.getValue();
                                    if (value.isJsonPrimitive()) {
                                        JsonPrimitive primitive = value.getAsJsonPrimitive();
                                        if (primitive.isNumber()) {
                                            ignoreNegativeDeltas = primitive.getAsBoolean();
                                            break;
                                        } else {
                                            throw new ValidationException(Messages.MapJsonThreshold_WrongFormatForIgnoreNegativeDeltas());
                                        }
                                    }
                                }
                            }
                        }
                        break;
                }
            }
        }

        boolean thresholdDetected = false;
        for (Map.Entry<String, JsonElement> enSchema : oSchema.entrySet()) {
            String schemaKey = enSchema.getKey();

            JsonElement schemaValue = enSchema.getValue();
            if (schemaValue.isJsonObject()) {
                JsonObject iSchemaObj = schemaValue.getAsJsonObject();

                type = getThresholdTag(iSchemaObj);

                if (type == ThresholdTags.tt_method) {
                    for (Map.Entry<String, JsonElement> enContent : oContent.entrySet()) {
                        if (schemaKey.equals(enContent.getKey())) {
                            JsonElement value = enContent.getValue();
                            if (value.isJsonPrimitive()) {
                                JsonPrimitive primitive = value.getAsJsonPrimitive();
                                if (primitive.isString()) {
                                    String method = primitive.getAsString().toLowerCase();
                                    if (method.equals("absolute")){
                                        AbsoluteThreshold thres = new AbsoluteThreshold(minimum, maximum);
                                        threshold = thres;
                                        thresholdDetected = true;
                                    } else if (method.equals("percentage")) {
                                        PercentageThreshold thres = new PercentageThreshold(percentage);
                                        threshold = thres;
                                        thresholdDetected = true;
                                    } else if (method.equals("delta")) {
                                        DeltaThreshold thres = new DeltaThreshold(delta);
                                        threshold = thres;
                                        thresholdDetected = true;
                                    } else if (method.equals("percentageaverage")) {
                                        PercentageAverageThreshold thres = new PercentageAverageThreshold(percentage);
                                        threshold = thres;
                                        thresholdDetected = true;
                                    } else if (method.equals("deltaaverage")){
                                        DeltaAverageThreshold thres = new DeltaAverageThreshold(delta);
                                        threshold = thres;
                                        thresholdDetected = true;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
                if (thresholdDetected)
                    break;
            }
        }
    }

    /**
     * Retrieve the type of threshold tag associated with 'type'
     * @param schemaObj Schema json object
     * @return Enum value for Group tag
     */
    private ThresholdTags getThresholdTag(JsonObject schemaObj){
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
        if (type.equals("method"))
            return ThresholdTags.tt_method;
        else if (type.equals("minimum"))
            return ThresholdTags.tt_minimum;
        else if (type.equals("maximum"))
            return ThresholdTags.tt_maximum;
        else if (type.equals("delta"))
            return ThresholdTags.tt_delta;
        else if (type.equals("percentage"))
            return ThresholdTags.tt_percentage;
        else if (type.equals("ignoreNegativeDeltas"))
            return ThresholdTags.tt_ignoreNegativeDeltas;
        else
            return ThresholdTags.tt_unknown;
    }

    // Getter

    public Threshold getThreshold() { return threshold; }
}
