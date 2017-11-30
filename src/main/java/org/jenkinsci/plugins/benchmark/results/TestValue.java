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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jenkinsci.plugins.benchmark.condensed.BooleanCondensed;
import org.jenkinsci.plugins.benchmark.condensed.DoubleCondensed;
import org.jenkinsci.plugins.benchmark.condensed.IntegerCondensed;
import org.jenkinsci.plugins.benchmark.condensed.StringCondensed;
import org.jenkinsci.plugins.benchmark.utilities.ContentDetected;
import org.jenkinsci.plugins.benchmark.utilities.TextToHTML;

import java.io.InvalidClassException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the core information of a standard test result
 *
 * @author Daniel Mercier
 * @since 5/10/2017
 */
public class TestValue extends TestGroup {

    // Public enumeration

    public enum ValueType {
        rt_unknown,
        rt_boolean,
        rt_integer,
        rt_double,
        rt_string
    }

    // Variables

    public static final String FAILED_STATE_COLOR = "#F37A7A";
    public static final String PASSED_STATE_COLOR = "#92D050";

    protected final ValueType           type;
    protected String                    group;
    protected String                    unit;

    protected final ConcurrentHashMap<Integer, TestProperty> properties = new ConcurrentHashMap<Integer, TestProperty>();

    // Constructor

    TestValue(TestGroup parent, String group, String name, String description, String unit, ValueType type) {
        super(parent, name, description, ClassType.ct_result);
        this.type = type;
        this.unit = unit;
        if (group == null){
            this.group = this.getParent().getFileSubGroupFullName();
        } else {
            this.group = group;
        }
    }

    TestValue(TestGroup parent, String group, String name, String description, String unit, ValueType type, ClassType ctype) {
        super(parent, name, description, ctype);
        this.type = type;
        this.unit = unit;
        if (group == null){
            this.group = this.getParent().getFileSubGroupFullName();
        } else {
            this.group = group;
        }
    }

    // Function

    /**
     * Convert a JSON object containing a condensed result to the plugin construct [DISPLAY LOADING]
     * @param object Json Object to convert
     * @param rootGroup Root group
     * @param fileList List of files
     * @param entityList List of generated entities
     * @param detected Key characteristics fo results
     */
    public static void convertCondensedResultJsonObject(JsonObject object, TestGroup rootGroup, Map<Integer, TestGroup> fileList, Map<Integer, TestValue> entityList, ContentDetected detected) {
        Integer         _hash = null;
        String          _name = null;
        String          _group = null;
        String          _description = null;
        String          _unit = null;
        Double          _previous = null;
        Double          _maximum = null;
        Double          _minimum = null;
        Double          _average = null;
        Double          _std_deviation = null;
        Integer         _passed = null;
        Integer         _failed = null;
        TestGroup       _file = rootGroup;
        ValueType       _type = ValueType.rt_unknown;

        for (Map.Entry<String, JsonElement> enObject : object.entrySet()) {
            String key = enObject.getKey().toLowerCase();
            if (key.equals("hash")) {
                JsonElement enElement = enObject.getValue();
                if (enElement.isJsonPrimitive()) {
                    JsonPrimitive primitive = enElement.getAsJsonPrimitive();
                    if (primitive.isNumber()) {
                        _hash = primitive.getAsInt();
                    }
                }
            } else if (key.equals("type")) {
                JsonElement value = enObject.getValue();
                if (value.isJsonPrimitive()) {
                    JsonPrimitive primitive = value.getAsJsonPrimitive();
                    if (primitive.isString()) {
                        _type = checkType(primitive.getAsString());
                    }
                }
            } else if (key.equals("name")) {
                JsonElement value = enObject.getValue();
                if (value.isJsonPrimitive()) {
                    JsonPrimitive primitive = value.getAsJsonPrimitive();
                    if (primitive.isString()) {
                        _name = primitive.getAsString();
                    }
                }
            } else if (key.equals("group")) {
                JsonElement value = enObject.getValue();
                if (value.isJsonPrimitive()) {
                    JsonPrimitive primitive = value.getAsJsonPrimitive();
                    if (primitive.isString()) {
                        _group = primitive.getAsString();
                    }
                }
            } else if (key.equals("description")) {
                JsonElement value = enObject.getValue();
                if (value.isJsonPrimitive()) {
                    JsonPrimitive primitive = value.getAsJsonPrimitive();
                    if (primitive.isString()) {
                        _description = primitive.getAsString();
                    }
                }
            } else if (key.equals("unit")) {
                JsonElement value = enObject.getValue();
                if (value.isJsonPrimitive()) {
                    JsonPrimitive primitive = value.getAsJsonPrimitive();
                    if (primitive.isString()) {
                        _unit = primitive.getAsString();
                        detected.setUnitsDetected(true);
                    }
                }
            }else if (key.equals("passed")) {
                JsonElement enElement = enObject.getValue();
                if (enElement.isJsonPrimitive()) {
                    JsonPrimitive primitive = enElement.getAsJsonPrimitive();
                    if (primitive.isNumber()) {
                        _passed = primitive.getAsInt();
                    }
                }
            } else if (key.equals("failed")) {
                JsonElement enElement = enObject.getValue();
                if (enElement.isJsonPrimitive()) {
                    JsonPrimitive primitive = enElement.getAsJsonPrimitive();
                    if (primitive.isNumber()) {
                        _failed = primitive.getAsInt();
                    }
                }
            } else if (key.equals("file")) {
                JsonElement inObject = enObject.getValue();
                if (inObject.isJsonPrimitive()) {
                    JsonPrimitive primitive = inObject.getAsJsonPrimitive();
                    if (primitive.isNumber()) {
                        TestGroup file = fileList.get(primitive.getAsInt());
                        if (file != null) {
                            _file = file;
                        }
                    }
                }
            } else if (key.equals("previous")) {
                JsonElement enElement = enObject.getValue();
                if (enElement.isJsonPrimitive()) {
                    JsonPrimitive primitive = enElement.getAsJsonPrimitive();
                    if (primitive.isNumber()) {
                        _previous = primitive.getAsDouble();
                    }
                }
            } else if (key.equals("minimum")) {
                JsonElement enElement = enObject.getValue();
                if (enElement.isJsonPrimitive()) {
                    JsonPrimitive primitive = enElement.getAsJsonPrimitive();
                    if (primitive.isNumber()) {
                        _minimum = primitive.getAsDouble();
                    }
                }
            } else if (key.equals("maximum")) {
                JsonElement enElement = enObject.getValue();
                if (enElement.isJsonPrimitive()) {
                    JsonPrimitive primitive = enElement.getAsJsonPrimitive();
                    if (primitive.isNumber()) {
                        _maximum = primitive.getAsDouble();
                    }
                }
            } else if (key.equals("average")) {
                JsonElement enElement = enObject.getValue();
                if (enElement.isJsonPrimitive()) {
                    JsonPrimitive primitive = enElement.getAsJsonPrimitive();
                    if (primitive.isNumber()) {
                        _average = primitive.getAsDouble();
                    }
                }
            } else if (key.equals("std_deviation")) {
                JsonElement enElement = enObject.getValue();
                if (enElement.isJsonPrimitive()) {
                    JsonPrimitive primitive = enElement.getAsJsonPrimitive();
                    if (primitive.isNumber()) {
                        _std_deviation = primitive.getAsDouble();
                    }
                }
            }
        }
        switch(_type) {
            case rt_integer:
                IntegerCondensed int_result = new IntegerCondensed(_file, _group,  _name, _description, _unit, _previous.intValue(), _minimum.intValue(), _maximum.intValue(), _average, _std_deviation, _passed, _failed);
                entityList.put(_hash, int_result);
                detected.setNumeralDetected(true);
                break;
            case rt_double:
                DoubleCondensed dbl_result = new DoubleCondensed(_file, _group,  _name, _description, _unit, _previous, _minimum, _maximum, _average, _std_deviation, _passed, _failed);
                entityList.put(_hash, dbl_result);
                detected.setNumeralDetected(true);
                break;
            case rt_string:
                StringCondensed str_result = new StringCondensed(_file, _group,  _name, _description, _unit, _passed, _failed);
                entityList.put(_hash, str_result);
                break;
            case rt_boolean:
                BooleanCondensed bool_result = new BooleanCondensed(_file, _group,  _name, _description, _unit, _passed, _failed);
                entityList.put(_hash, bool_result);
                break;
            default:
        }
        if (_group != null){
            detected.setGroupDetected(true);
        }
    }

    /**
     * Convert a JSON object containing a parameter to the plugin construct [DISPLAY LOADING]
     * @param object Json Object to convert
     * @param rootGroup Root group
     * @param entityList List of generated entities
     * @param detected Key characteristics fo results
     */
    public static void convertCondensedParameterJsonObject(JsonObject object, TestGroup rootGroup, Map<Integer, TestValue> entityList, ContentDetected detected) {
        Integer         _hash = null;
        String          _name = null;
        String          _group = null;
        String          _description = null;
        String          _unit = null;
        TestGroup       _file = rootGroup;
        ValueType       _type = ValueType.rt_unknown;

        for (Map.Entry<String, JsonElement> enObject : object.entrySet()) {
            String key = enObject.getKey().toLowerCase();
            if (key.equals("hash")) {
                JsonElement enElement = enObject.getValue();
                if (enElement.isJsonPrimitive()) {
                    JsonPrimitive primitive = enElement.getAsJsonPrimitive();
                    if (primitive.isNumber()) {
                        _hash = primitive.getAsInt();
                    }
                }
            } else if (key.equals("type")) {
                JsonElement value = enObject.getValue();
                if (value.isJsonPrimitive()) {
                    JsonPrimitive primitive = value.getAsJsonPrimitive();
                    if (primitive.isString()) {
                        _type = checkType(primitive.getAsString());
                    }
                }
            } else if (key.equals("name")) {
                JsonElement value = enObject.getValue();
                if (value.isJsonPrimitive()) {
                    JsonPrimitive primitive = value.getAsJsonPrimitive();
                    if (primitive.isString()) {
                        _name = primitive.getAsString();
                    }
                }
            } else if (key.equals("group")) {
                JsonElement value = enObject.getValue();
                if (value.isJsonPrimitive()) {
                    JsonPrimitive primitive = value.getAsJsonPrimitive();
                    if (primitive.isString()) {
                        _group = primitive.getAsString();
                    }
                }
            } else if (key.equals("description")) {
                JsonElement value = enObject.getValue();
                if (value.isJsonPrimitive()) {
                    JsonPrimitive primitive = value.getAsJsonPrimitive();
                    if (primitive.isString()) {
                        _description = primitive.getAsString();
                    }
                }
            } else if (key.equals("unit")) {
                JsonElement value = enObject.getValue();
                if (value.isJsonPrimitive()) {
                    JsonPrimitive primitive = value.getAsJsonPrimitive();
                    if (primitive.isString()) {
                        _unit = primitive.getAsString();
                    }
                }
            }
        }
        switch(_type) {
            case rt_integer:
                IntegerValue int_result = new IntegerValue(_file, _group,  _name, _description, _unit);
                entityList.put(_hash, int_result);
                break;
            case rt_double:
                DoubleValue dbl_result = new DoubleValue(_file,  _group,  _name, _description, _unit);
                entityList.put(_hash, dbl_result);
                break;
            case rt_string:
                StringValue str_result = new StringValue(_file, _group,  _name, _description, _unit);
                entityList.put(_hash, str_result);
                break;
            case rt_boolean:
                BooleanValue bool_result = new BooleanValue(_file, _group,  _name, _description, _unit);
                entityList.put(_hash, bool_result);
                break;
            default:
        }
    }

    /**
     * Convert result/parameter content to the plug-in construct [DISPLAY LOAD]
     * @param build Build number
     * @param object Object to convert
     * @param rootGroup Root group
     * @param fileList List of files
     * @param entityList List of result/parameter entities
     * @param paramList List of parameters
     */
    public static void convertResultJsonObject(int build, JsonObject object, TestGroup rootGroup, Map<Integer, TestGroup> fileList, Map<Integer, TestValue> entityList, Map<Integer, TestValue> paramList) {

        Integer             _hash = null;
        Integer             _id = null;
        Boolean             _failedState = null;
        JsonPrimitive       _value = null;
        Map<String, String> _messages = new HashMap<String, String>();
        List<TestValue>     _parameters = new ArrayList<TestValue>();


        for (Map.Entry<String, JsonElement> enObject : object.entrySet()) {
            String key = enObject.getKey().toLowerCase();
            if (key.equals("hash")) {
                JsonElement enElement = enObject.getValue();
                if (enElement.isJsonPrimitive()) {
                    JsonPrimitive primitive = enElement.getAsJsonPrimitive();
                    if (primitive.isNumber()) {
                        _hash = primitive.getAsInt();
                    }
                }
            } else if (key.equals("value")) {
                JsonElement enElement = enObject.getValue();
                if (enElement.isJsonPrimitive()) {
                    _value = enElement.getAsJsonPrimitive();
                }
            } else if (key.equals("id")) {
                JsonElement enElement = enObject.getValue();
                if (enElement.isJsonPrimitive()) {
                    JsonPrimitive primitive = enElement.getAsJsonPrimitive();
                    if (primitive.isNumber()) {
                        _id = primitive.getAsInt();
                    }
                }
            } else if (key.equals("failedstate")) {
                JsonElement enElement = enObject.getValue();
                if (enElement.isJsonPrimitive()) {
                    JsonPrimitive primitive = enElement.getAsJsonPrimitive();
                    if (primitive.isBoolean()) {
                        _failedState = primitive.getAsBoolean();
                    }
                }
            } else if (key.equals("messages")) {
                JsonElement inElement = enObject.getValue();
                if (inElement.isJsonArray()) {
                    JsonArray inArray = inElement.getAsJsonArray();
                    for (JsonElement enArray : inArray) {
                        if (enArray.isJsonObject()) {
                            String _title = "";
                            String _message = "";
                            JsonObject aObject = enArray.getAsJsonObject();
                            for (Map.Entry<String, JsonElement> enSObject : aObject.entrySet()) {
                                if (enSObject.getKey().equalsIgnoreCase("title")) {
                                    JsonElement enElement = enSObject.getValue();
                                    if (enElement.isJsonPrimitive()) {
                                        JsonPrimitive primitive = enElement.getAsJsonPrimitive();
                                        if (primitive.isString()) {
                                            _title = primitive.getAsString();
                                        }
                                    }
                                } else if (enSObject.getKey().equalsIgnoreCase("message")) {
                                    JsonElement enElement = enSObject.getValue();
                                    if (enElement.isJsonPrimitive()) {
                                        JsonPrimitive primitive = enElement.getAsJsonPrimitive();
                                        if (primitive.isString()) {
                                            _message = primitive.getAsString();
                                        }
                                    }
                                }
                            }
                            _messages.put(_title, _message);
                        }
                    }
                }
            } else if (key.equals("parameters")) {
                JsonElement inElement = enObject.getValue();
                if (inElement.isJsonArray()) {
                    JsonArray inArray = inElement.getAsJsonArray();
                    for (JsonElement enArray : inArray) {
                        if (enArray.isJsonPrimitive()) {
                            JsonPrimitive primitive = enArray.getAsJsonPrimitive();
                            if (primitive.isNumber()) {
                                TestValue parameter = paramList.get(primitive.getAsInt());
                                if (parameter != null) {
                                    _parameters.add(parameter);
                                }
                            }
                        }
                    }
                }
            }
        }
        TestValue res = entityList.get(_hash);
        if (res == null){
            String      _name = null;
            String      _group = null;
            String      _description = null;
            String      _unit = null;
            ValueType   _type = ValueType.rt_unknown;
            TestGroup  _file = rootGroup;
            for (Map.Entry<String, JsonElement> enObject : object.entrySet()) {
                String key = enObject.getKey().toLowerCase();
                if (key.equals("name")) {
                    JsonElement value = enObject.getValue();
                    if (value.isJsonPrimitive()) {
                        JsonPrimitive primitive = value.getAsJsonPrimitive();
                        if (primitive.isString()) {
                            _name = primitive.getAsString();
                        }
                    }
                } else if (key.equals("group")) {
                    JsonElement value = enObject.getValue();
                    if (value.isJsonPrimitive()) {
                        JsonPrimitive primitive = value.getAsJsonPrimitive();
                        if (primitive.isString()) {
                            _group = primitive.getAsString();
                        }
                    }
                } else if (key.equals("description")) {
                    JsonElement value = enObject.getValue();
                    if (value.isJsonPrimitive()) {
                        JsonPrimitive primitive = value.getAsJsonPrimitive();
                        if (primitive.isString()) {
                            _description = primitive.getAsString();
                        }
                    }
                } else if (key.equals("unit")) {
                    JsonElement value = enObject.getValue();
                    if (value.isJsonPrimitive()) {
                        JsonPrimitive primitive = value.getAsJsonPrimitive();
                        if (primitive.isString()) {
                            _unit = primitive.getAsString();
                        }
                    }
                } else if (key.equals("type")) {
                    JsonElement value = enObject.getValue();
                    if (value.isJsonPrimitive()) {
                        JsonPrimitive primitive = value.getAsJsonPrimitive();
                        if (primitive.isString()) {
                            _type = checkType(primitive.getAsString());
                        }
                    }
                } else if (key.equals("file")) {
                JsonElement inObject = enObject.getValue();
                if (inObject.isJsonPrimitive()) {
                    JsonPrimitive primitive = inObject.getAsJsonPrimitive();
                    if (primitive.isNumber()) {
                        TestGroup file = fileList.get(primitive.getAsInt());
                        if (file != null) {
                            _file = file;
                        }
                    }
                }
            }
            }
            switch (_type) {
                case rt_boolean:
                    BooleanValue bool_value = new BooleanValue (_file, null, _name, _description, _unit);
                    bool_value.setValue(build, _value.getAsBoolean());
                    bool_value.setGroup(_group);
                    entityList.put(_hash, bool_value);
                    if (paramList != null) {
                        _file.addGroup(bool_value);
                    }
                    res = bool_value;
                    break;

                case rt_string:
                    StringValue str_value = new StringValue (_file, null, _name, _description, _unit);
                    str_value.setValue(build, _value.getAsString());
                    str_value.setGroup(_group);
                    entityList.put(_hash, str_value);
                    if (paramList != null) {
                        _file.addGroup(str_value);
                    }
                    res = str_value;
                    break;

                case rt_double:
                    DoubleValue dbl_value = new DoubleValue (_file, null, _name, _description, _unit);
                    dbl_value.setValue(build, _value.getAsDouble());
                    dbl_value.setGroup(_group);
                    entityList.put(_hash, dbl_value);
                    if (paramList != null) {
                        _file.addGroup(dbl_value);
                    }
                    res = dbl_value;
                    break;

                case rt_integer:
                    IntegerValue int_value = new IntegerValue (_file, null, _name, _description, _unit);
                    int_value.setValue(build, _value.getAsInt());
                    int_value.setGroup(_group);
                    entityList.put(_hash, int_value);
                    if (paramList != null) {
                        _file.addGroup(int_value);
                    }
                    res = int_value;
                    break;

                default:
            }
        }
        if (res != null){
            if (_value != null){
                ValueType type = res.getType();
                switch (type) {
                    case rt_boolean:
                        if (_value.isBoolean()) {
                            BooleanValue value = (BooleanValue) res;
                            value.setValue(build, _value.getAsBoolean());
                        }
                        break;

                    case rt_double:
                        if (_value.isNumber()) {
                            DoubleValue value = (DoubleValue) res;
                            value.setValue(build, _value.getAsDouble());
                        }
                        break;

                    case rt_integer:
                        if (_value.isNumber()) {
                            IntegerValue value = (IntegerValue) res;
                            value.setValue(build, _value.getAsInt());
                        }
                        break;

                    case rt_string:
                        if (_value.isNumber()) {
                            StringValue value = (StringValue) res;
                            value.setValue(build, _value.getAsString());
                        }
                        break;

                    default:
                }
            }
            if (_failedState != null) {
                res.setFailedState(build, _failedState);
            }
            if (_id != null) {
                res.setId(build, _id);
            }
            if (_messages != null) {
                res.setMessages(build, _messages);
            }
            if (!_parameters.isEmpty()) {
                res.setParameters(build, _parameters);
            }
        }
    }

    /**
     * Convert result/parameter content to the plug-in construct [DISPLAY LOAD]
     * @param build Build number
     * @param object Object to convert
     * @param rootGroup Root group
     * @param entityList List of result/parameter entities
     */
    public static void convertParameterJsonObject(int build, JsonObject object, TestGroup rootGroup, Map<Integer, TestValue> entityList) {

        Integer             _hash = null;
        TestGroup           _file = rootGroup;
        JsonPrimitive       _value = null;

        for (Map.Entry<String, JsonElement> enObject : object.entrySet()) {
            String key = enObject.getKey().toLowerCase();
            if (key.equals("hash")) {
                JsonElement enElement = enObject.getValue();
                if (enElement.isJsonPrimitive()) {
                    JsonPrimitive primitive = enElement.getAsJsonPrimitive();
                    if (primitive.isNumber()) {
                        _hash = primitive.getAsInt();
                    }
                }
            } else if (key.equals("value")) {
                JsonElement enElement = enObject.getValue();
                if (enElement.isJsonPrimitive()) {
                    _value = enElement.getAsJsonPrimitive();
                }
            }
        }
        TestValue res = entityList.get(_hash);
        if (res == null){
            String      _name = null;
            String      _group = null;
            String      _description = null;
            String      _unit = null;
            ValueType   _type = ValueType.rt_unknown;
            for (Map.Entry<String, JsonElement> enObject : object.entrySet()) {
                String key = enObject.getKey().toLowerCase();
                if (key.equals("name")) {
                    JsonElement value = enObject.getValue();
                    if (value.isJsonPrimitive()) {
                        JsonPrimitive primitive = value.getAsJsonPrimitive();
                        if (primitive.isString()) {
                            _name = primitive.getAsString();
                        }
                    }
                } else if (key.equals("group")) {
                    JsonElement value = enObject.getValue();
                    if (value.isJsonPrimitive()) {
                        JsonPrimitive primitive = value.getAsJsonPrimitive();
                        if (primitive.isString()) {
                            _group = primitive.getAsString();
                        }
                    }
                } else if (key.equals("description")) {
                    JsonElement value = enObject.getValue();
                    if (value.isJsonPrimitive()) {
                        JsonPrimitive primitive = value.getAsJsonPrimitive();
                        if (primitive.isString()) {
                            _description = primitive.getAsString();
                        }
                    }
                } else if (key.equals("unit")) {
                    JsonElement value = enObject.getValue();
                    if (value.isJsonPrimitive()) {
                        JsonPrimitive primitive = value.getAsJsonPrimitive();
                        if (primitive.isString()) {
                            _unit = primitive.getAsString();
                        }
                    }
                } else if (key.equals("type")) {
                    JsonElement value = enObject.getValue();
                    if (value.isJsonPrimitive()) {
                        JsonPrimitive primitive = value.getAsJsonPrimitive();
                        if (primitive.isString()) {
                            _type = checkType(primitive.getAsString());
                        }
                    }
                }
            }
            switch (_type) {
                case rt_boolean:
                    BooleanValue bool_value = new BooleanValue (_file, null, _name, _description, _unit);
                    bool_value.setValue(build, _value.getAsBoolean());
                    bool_value.setGroup(_group);
                    entityList.put(_hash, bool_value);
                    res = bool_value;
                    break;

                case rt_string:
                    StringValue str_value = new StringValue (_file, null, _name, _description, _unit);
                    str_value.setValue(build, _value.getAsString());
                    str_value.setGroup(_group);
                    entityList.put(_hash, str_value);
                    res = str_value;
                    break;

                case rt_double:
                    DoubleValue dbl_value = new DoubleValue (_file, null, _name, _description, _unit);
                    dbl_value.setValue(build, _value.getAsDouble());
                    dbl_value.setGroup(_group);
                    entityList.put(_hash, dbl_value);
                    res = dbl_value;
                    break;

                case rt_integer:
                    IntegerValue int_value = new IntegerValue (_file, null, _name, _description, _unit);
                    int_value.setValue(build, _value.getAsInt());
                    int_value.setGroup(_group);
                    entityList.put(_hash, int_value);
                    res = int_value;
                    break;
                default:
            }
        }
        if (res != null){
            if (_value != null){
                ValueType type = res.getType();
                switch (type) {
                    case rt_boolean:
                        if (_value.isBoolean()) {
                            BooleanValue value = (BooleanValue) res;
                            value.setValue(build, _value.getAsBoolean());
                        }
                        break;

                    case rt_double:
                        if (_value.isNumber()) {
                            DoubleValue value = (DoubleValue) res;
                            value.setValue(build, _value.getAsDouble());
                        }
                        break;

                    case rt_integer:
                        if (_value.isNumber()) {
                            IntegerValue value = (IntegerValue) res;
                            value.setValue(build, _value.getAsInt());
                        }
                        break;

                    case rt_string:
                        if (_value.isNumber()) {
                            StringValue value = (StringValue) res;
                            value.setValue(build, _value.getAsString());
                        }
                        break;
                    default:
                }
            }
        }
    }

    /**
     * Generate the HTML table content for this result [DETAIL PAGE]
     * @param builds List of build numbers
     * @param decimalSeparator Decimal Separator
     * @return HTML content for this result
     */
    public String getHTMLResult(TreeSet<Integer> builds, char decimalSeparator) {
        StringBuffer content = new StringBuffer();
        for (int build = builds.last(); build >= builds.first(); build--) {
            String value = this.getValueAsLocaleString(build, decimalSeparator);
            if (value.isEmpty()) {
                content.append("<td>-</td>");
            } else {
                Boolean state = this.getFailedState(build);
                if (state == null){
                    content.append("<td>");
                    content.append(value.toString());
                    content.append("</td>");
                } else {
                    content.append("<td style=\"background-color:");
                    content.append(this.getColor(state));
                    content.append(";\">");
                    content.append(value.toString());
                    content.append("</td>");
                }
            }
        }
        return content.toString();
    }


    /**
     * Export result property to Json object [EXPORT RAW]
     * Works in combination with getJsonObject() from result specific formats
     * @param hash Result hash
     * @return Json Object
     */
    @Override
    public JsonObject getJsonObject(int hash){
        JsonObject object = new JsonObject();
        if (this.getName() != null){
            object.addProperty("hash", hash);
        }
        if (this.getId() != null) {
            object.addProperty("id", this.getId());
        }
        if (this.getFailedState() != null) {
            object.addProperty("failedState",  this.getFailedState());
        }
        Map<String,String> messages =  this.getMessages();
        if (messages != null) {
            if(messages.size() > 0) {
                JsonArray arrayMessages = new JsonArray();
                for (Map.Entry<String, String> message : this.getMessages().entrySet()) {
                    JsonObject objectMessage = new JsonObject();
                    objectMessage.addProperty("title", message.getKey());
                    objectMessage.addProperty("message", message.getValue());
                    arrayMessages.add(objectMessage);
                }
                object.add("messages", arrayMessages);
            }
        }
        if (this.ctype == ClassType.ct_result) {
            boolean detParameters = false;
            JsonArray arrayParameters = new JsonArray();
            List<TestGroup> parameters = this.getAllConnectedParameters();
            for (TestGroup parameter:parameters) {
                arrayParameters.add(parameter.getGroupHash());
                detParameters = true;
            }
            if (detParameters) {
                object.add("parameters", arrayParameters);
            }
        }
        return object;
    }

    /**
     * Create an JSON object from a parameter [EXPORT CONDENSED]
     * @param hash Result hash
     * @return JSON object
     */
    public JsonObject getParameterJsonObject(int hash) {
        // Assemble JSON object
        JsonObject object = new JsonObject();
        object.addProperty("hash", hash);
        if ( this.getFileGroup() != null) {
            object.addProperty("file", this.getFileGroup().getGroupHash());
        }
        if(group != null && !this.group.isEmpty()) {
            object.addProperty("group", this.group);
        }
        object.addProperty("name", this.name);
        if(this.description != null && !this.description.isEmpty()) {
            object.addProperty("description", this.description);
        }
        object.addProperty("type", outputType(type));
        if (this.getUnit() != null && !this.getUnit().isEmpty()){
            object.addProperty("unit", this.getUnit());
        }
        return object;
    }

    /**
     * Generate the HTML detail content for this result and for a determined build [DETAIL PAGE]
     * @param build Build number
     * @param decimalSeparator Decimal Separator
     * @return HTML detail for this result
     */
    public String getHTMLDetails(Integer build, char decimalSeparator) {
        StringBuffer content = new StringBuffer();
        if (description != null && !description.isEmpty()) {
            content.append("<tr><td>");
            content.append(Messages.Description());
            content.append("</td><td>");
            content.append(this.description);
            content.append("</td></tr>");
        }
        String value = this.getValueAsLocaleString(build, decimalSeparator);
        if (value.isEmpty()) {
            content.append("<tr><td>");
            content.append(Messages.Value());
            content.append("</td><td>");
            content.append(Messages.NotApplicableShort());
            content.append("</td></tr>");
        } else {
            content.append("<tr><td>");
            content.append(Messages.Value());
            content.append("</td><td>");
            content.append(value);
            content.append("</td></tr>");
        }
        if (this.unit != null && !this.unit.isEmpty()) {
            content.append("<tr><td>");
            content.append(Messages.Unit());
            content.append("</td><td>");
            content.append(this.unit);
            content.append("</td></tr>");
        }
        Boolean failedState = this.getFailedState(build);
        if (failedState != null) {
            if (failedState) {
                content.append("<tr><td>");
                content.append(Messages.State());
                content.append("</td><td style=\"color:");
                content.append(this.getColor(failedState));
                content.append(";\"><b>");
                content.append(Messages.Failed());
                content.append("</b></td></tr>");
            } else {
                content.append("<tr><td>");
                content.append(Messages.State());
                content.append("</td><td style=\"color:");
                content.append(this.getColor(failedState));
                content.append(";\"><b>");
                content.append(Messages.Passed());
                content.append("</b></td></tr>");
            }
        }
        Integer id = this.getId(build);
        if (id != null) {
            content.append("<tr><td>");
            content.append(Messages.AttachedId());
            content.append("</td><td>");
            content.append(id.toString());
            content.append("</td></tr>");
        }
        // Messages by nature should use as much width so the content is contained in one cell row.
        Map<String,String> messages = this.getMessages(build);
        if (messages != null && messages.size() != 0) {
            for(Map.Entry<String,String> entry: messages.entrySet()){
                content.append("<tr><td colspan=\"2\">");
                content.append(entry.getKey());
                content.append(":<p>");
                content.append(TextToHTML.toHTML(entry.getValue()));
                content.append("<p></td></tr>");
            }
        }
        return content.toString();
    }

    /**
     * Generate the HTML raw table content for this result [TABLE PAGE]
     * @param key Result hash value
     * @param detected Key characteristic of this set of results
     * @param builds List of build numbers
     * @param listNPassed Number of passed result test for each build
     * @param listNFailed Number of passed result test for each build
     * @param decimalSeparator Decimal Separator
     * @return HTML table content for this result
     */
    public String getHTMLResult(Integer key, ContentDetected detected, TreeSet<Integer> builds, List<Integer> listNPassed, List<Integer> listNFailed, char decimalSeparator) {
        StringBuffer content = new StringBuffer();
        content.append("<tr><td>");
        if (detected.isFileDetected()) {
            if (this.getParent() == null){
                content.append("</td><td>");
            } else {
                String name = this.getParent().getName();
                if ( name.equalsIgnoreCase("__root__")){
                    content.append("</td><td>");
                } else {
                    content.append(name);
                    content.append("</td><td>");
                }
            }
        }
        if (detected.isGroupDetected()) {
            if (this.getGroup() == null){
                content.append("</td><td>");
            } else {
                content.append(this.getGroup());
                content.append("</td><td>");
            }
        }
        content.append(this.getName());
        content.append("</td><td>");
        if (detected.isUnitsDetected()) {
            if (this.unit != null && !this.unit.isEmpty()) {
                content.append(this.unit);
                content.append("</td><td>");
            } else {
                content.append("-</td><td>");
            }
        }
        content.append(key.toString());
        int index = 0;
        for (int build = builds.last(); build >= builds.first(); build--) {
            String value = this.getValueAsLocaleString(build, decimalSeparator);
            if (value.isEmpty()) {
                content.append("</td><td>-");
            } else {
                Boolean failedState = this.getFailedState(build);
                if (failedState == null){
                    content.append("</td><td>");
                    content.append(value);
                } else {
                    content.append("</td><td style=\"background-color:");
                    content.append(this.getColor(failedState));
                    content.append(";\">");
                    content.append(value);
                    if (failedState){
                        listNFailed.set(index, listNFailed.get(index) + 1);
                    } else {
                        listNPassed.set(index, listNPassed.get(index) + 1);
                    }
                }
            }
            index++;
        }
        content.append("</td></tr>");
        return content.toString();
    }

    /**
     * Generate the CSV table content for this result [CSV EXPORT]
     * @param builds List of build numbers
     * @param detected Key characteristic of this set of results
     * @return CSV table content for result
     */
    public String getCSVResult(TreeSet<Integer> builds, ContentDetected detected) {
        StringBuffer content = new StringBuffer();
        if (detected.isFileDetected()) {
            if (this.getParent() == null){
                content.append(',');
            } else {
                String name = this.getParent().getName();
                if ( name.equalsIgnoreCase("__root__")){
                    content.append(',');
                } else {
                    content.append(name);
                    content.append(',');
                }
            }
        }
        if (detected.isGroupDetected()) {
            if (this.getGroup() == null){
                content.append(',');
            } else {
                content.append(this.getGroup());
                content.append(',');
            }
        }
        content.append(this.getName());
        if (detected.isUnitsDetected()) {
            if (this.unit == null) {
                content.append(",-");
            } else {
                content.append(',');
                content.append(this.unit);
            }
        }
        for (int build = builds.last(); build >= builds.first(); build--) {
            String value = this.getValueAsString(build);
            if (value == null || value.isEmpty()) {
                content.append(",-");
            } else {
                content.append(',');
                content.append(value);
            }
        }
        return content.toString();
    }

    /**
     * Generate the CSV state content to describe this result state [CSV EXPORT]
     * @param key Result hash value
     * @param detected Key characteristic of this set of results
     * @param builds List of build numbers
     * @return CSV table state content for result
     */
    public String getCSVResultState(Integer key, ContentDetected detected, TreeSet<Integer> builds ) {
        StringBuffer content = new StringBuffer();
        if (detected.isFileDetected()) {
            if (this.getParent() == null){
                content.append(',');
            } else {
                String name = this.getParent().getName();
                if ( name.equalsIgnoreCase("__root__")){
                    content.append(',');
                } else {
                    content.append(name);
                    content.append(',');
                }
            }
        }
        if (detected.isGroupDetected()) {
            if (this.getGroup() == null){
                content.append(',');
            } else {
                content.append(this.getGroup());
                content.append(',');
            }
        }
        content.append(this.getName());
        if (detected.isUnitsDetected()) {
            if (this.unit != null && !this.unit.isEmpty()) {
                content.append(',');
                content.append(this.unit);
            } else {
                content.append(",-");
            }
        }
        for (int build = builds.last(); build >= builds.first(); build--) {
            Boolean state = this.getFailedState(build);
            if (state == null){
                content.append(",-");
            } else {
                if (state == false) {
                    content.append(',');
                    content.append(Messages.Passed());
                } else {
                    content.append(',');
                    content.append(Messages.Failed());
                }
            }
        }
        return content.toString();
    }

    /**
     * Generate the HTML content for all the parameters for this result at a determined build [DETAIL PAGE]
     * @param build Build number
     * @param decimalSeparator Decimal Separator
     * @return HTML content for all parameters
     */
    public String getHTMLParameters(Integer build, char decimalSeparator) {
        StringBuffer content = new StringBuffer();
        List<TestValue> parameters = getParameters(build);
        if (parameters != null && parameters.size() != 0){
            Integer i = 1;
            for (TestValue parameter : parameters) {
                content.append(parameter.getHTMLParameter(i, build, decimalSeparator));
                i++;
            }
        }
        return content.toString();
    }

    /**
     * Generate the HTML content for this parameter [DETAIL PAGE]
     * @param number Displayed parameter number
     * @param build Build number
     * @param decimalSeparator Decimal Separator
     * @return HTML content for this parameter
     */
    public String getHTMLParameter(Integer number, Integer build, char decimalSeparator) {
        StringBuffer content = new StringBuffer();
        content.append("<tr><td><b>");
        content.append(Messages.ParameterNumber(number.toString()));
        content.append("</b></td><td></td></tr><tr><td>");
        content.append(Messages.Name());
        content.append("</td><td>");
        content.append(this.getName());
        content.append("</td></tr>");

        if (description != null && !description.isEmpty()) {
            content.append("<tr><td>");
            content.append(Messages.Description());
            content.append("</td><td>");
            content.append(this.description);
            content.append("</td></tr>");
        }

        content.append("<tr><td>");
        content.append(Messages.Value());
        content.append("</td><td>");
        String value = this.getValueAsLocaleString(build, decimalSeparator);
        if (value.isEmpty()) {
            content.append(Messages.NotApplicableShort());
        } else {
            content.append(value);
        }
        content.append("</td></tr>");
        if (this.unit != null && !this.unit.isEmpty()) {
            content.append("<tr><td>");
            content.append(Messages.Unit());
            content.append("</td><td>");
            content.append(this.unit);
            content.append("</td></tr>");
        }
        return content.toString();
    }

    /**
     * Assemble the HTML content to display the condensed table [TABLE PAGE]
     * @param detected Key characteristics of results
     * @param decimalSeparator Decimal separator
     * @return HTML content
     */
    public String getHTMLCondensed(ContentDetected detected, char decimalSeparator) { return ""; }

    /**
     * Assemble the HTML content to display the condensed table [DETAIL PAGE]
     * @param detected Key characteristics of results
     * @param decimalSeparator Decimal separator
     * @return HTML content
     */
    public String getHTMLCondensedDetail(ContentDetected detected, char decimalSeparator) { return ""; }

    /**
     * Assemble the CSV content to display the condensed table [CSV EXPORT]
     * @param detected Key characteristics of results
     * @return CSV content
     */
    public String getCSVCondensed(ContentDetected detected) { return ""; }

    /**
     * Create an JSON object with the condensed information of this result [EXPORT CONDENSED]
     * @param build Build number
     * @param hash Result hash
     * @return JSON object
     */
    public JsonObject getCondensedJsonObject (int build, int hash) { return null; }

    /**
     * Return whether the TestValue is based on numeral values
     * @return Whether class is numeral
     */
    public Boolean isNumeral(){
        switch(type){
            case rt_double:
            case rt_integer:
                return true;
            default:
                return false;
        }
    }

    /**
     * Check attached thresholds to verify result validity.
     * @param previous Previous value
     * @param average Calculated average
     */
    public void checkThresholdStatus(Double previous, Double average) { }

    // Setters

    public void setId(Integer id){
        setId(0, id);
    }

    public void setId(int build, Integer id) {
        if (id == null) {
            return;
        } else {
            TestProperty properties = this.properties.get(build);
            if (properties == null) {
                properties = new TestProperty();
                properties.setId(id);
                this.properties.put(build, properties);
            } else {
                properties.setId(id);
            }
        }
    }

    public void setFailedState(Boolean failed){
        setFailedState(0, failed);
    }

    public void setFailedState(int build, Boolean failed) {
        if (failed == null){
            return;
        } else {
            TestProperty properties = this.properties.get(build);
            if (properties == null) {
                properties = new TestProperty();
                properties.setFailedState(failed);
                this.properties.put(build, properties);
            } else {
                if (getFailedState() == null) {
                    properties.setFailedState(failed);
                } else if ((getFailedState() == false && failed == true)) {
                    properties.setFailedState(failed);
                }
            }
        }
    }

    public void setMessage(String title, String message){
        setMessage(0, title, message);
    }

    public void setMessage(int build, String title, String message) {
        TestProperty properties = this.properties.get(build);
        if (properties == null) {
            properties = new TestProperty();
            properties.addMessage(title, message);
            this.properties.put( build, properties);
        } else {
            properties.addMessage(title, message);
        }
    }

    public void setMessages(Map<String, String> messages) {
        setMessages(0, messages);
    }

    public void setMessages(int build, Map<String, String> messages) {
        if (messages == null || messages.size() == 0){
            return;
        } else {
            TestProperty properties = this.properties.get(build);
            if (properties == null) {
                properties = new TestProperty();
                properties.addMessages(messages);
                this.properties.put(build, properties);
            } else {
                properties.addMessages(messages);
            }
        }
    }

    public void setParameter(TestValue parameter) {
        setParameter(0, parameter);
    }

    public void  setParameter(int build, TestValue parameter) {
        if (parameter == null) {
            return;
        } else {
            TestProperty properties = this.properties.get(build);
            if (properties == null) {
                properties = new TestProperty();
                properties.addParameter(parameter);
                this.properties.put(build, properties);
            } else {
                properties.addParameter(parameter);
            }
        }
    }

    public void setParameters(List<TestValue> parameters) {
        setParameters(0, parameters);
    }

    public void  setParameters(int build, List<TestValue> parameters) {
        if (parameters == null || parameters.size() == 0) {
            return;
        } else {
            TestProperty properties = this.properties.get(build);
            if (properties == null) {
                properties = new TestProperty();
                properties.addParameters(parameters);
                this.properties.put(build, properties);
            } else {
                properties.addParameters(parameters);
            }
        }
    }

    public void setGroup() { this.group = this.getParent().getFileSubGroupFullName(); }
    public void setGroup(String group) { this.group = group; }

    // Getters

    public ValueType getType() { return type; }
    public String getGroup() { return group; }
    public String getUnit() { return unit; }

    public ConcurrentHashMap<Integer, TestProperty> getProperties() { return properties; }

    public String getValueAsString(int build) { return ""; }
    public String getValueAsLocaleString(int build, char decimalSeparator) { return ""; }

    public Boolean getFailedState() {
        TestProperty property = this.properties.get(0);
        if (property == null) {
            return null;
        } else {
            return property.getFailedState();
        }
    }
    public Boolean getFailedState(int build) {
        TestProperty property = this.properties.get(build);
        if (property == null){
            return null;
        } else {
            return property.getFailedState();
        }
    }

    public Integer getId() {
        TestProperty property = this.properties.get(0);
        if (property == null) {
            return null;
        } else {
            return property.getId();
        }
    }
    public Integer getId(int build) {
        TestProperty property = this.properties.get(build);
        if (property == null) {
            return null;
        } else {
            return property.getId();
        }
    }

    public Map<String,String> getMessages() {
        TestProperty property = this.properties.get(0);
        if (property == null) {
            return null;
        } else {
            return property.getMessages();
        }
    }
    public Map<String,String> getMessages(int build) {
        TestProperty property = this.properties.get(build);
        if (property == null) {
            return null;
        } else {
            return property.getMessages();
        }
    }

    public List<TestValue> getParameters() {
        TestProperty property = this.properties.get(0);
        if (property == null) {
            return null;
        } else {
            return property.getParameters();
        }
    }

    public List<TestValue> getParameters(int build) {
        TestProperty property = this.properties.get(build);
        if (property == null) {
            return null;
        } else {
            return property.getParameters();
        }
    }

    protected String getColor(Boolean failedState){
        if (failedState) {
            return FAILED_STATE_COLOR;
        } else {
            return PASSED_STATE_COLOR;
        }
    }

    public int getNumberOfProperties() { return properties.size(); }

    protected static ValueType checkType(String type) {
        type = type.toLowerCase();
        if (type.equals("double")) {
            return ValueType.rt_double;
        } else if (type.equals("boolean")) {
            return ValueType.rt_boolean;
        } else if (type.equals("integer")) {
            return ValueType.rt_integer;
        } else if (type.equals("string")) {
            return ValueType.rt_string;
        }
        return ValueType.rt_unknown;
    }

    protected static String outputType(ValueType type) {
        switch (type) {
            case rt_double:
                return "double";
            case rt_boolean:
                return "boolean";
            case rt_integer:
                return "integer";
            case rt_string:
                return "string";
            default:
                return "unknown";
        }
    }

    public JsonArray getDataAsJsonArray(TreeSet<Integer> buildNumbers) throws InvalidClassException { throw new InvalidClassException(Messages.TestValue_TestValueNotNumeral());}
}
