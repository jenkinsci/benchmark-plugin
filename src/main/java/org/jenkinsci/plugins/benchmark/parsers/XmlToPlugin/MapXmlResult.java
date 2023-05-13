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
package org.jenkinsci.plugins.benchmark.parsers.XmlToPlugin;

import org.jenkinsci.plugins.benchmark.exceptions.ValidationException;
import org.jenkinsci.plugins.benchmark.results.*;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Map Parameter XML schema/content data to Jenkins plugin data construct
 *
 * @author Daniel Mercier
 * @since 6/20/2017.
 */
public class MapXmlResult {

    /**
     * Enumeration for Group tags
     */
    private enum ResultTags {
        rt_empty,
        rt_unknown,
        rt_id,
        rt_name,
        rt_description,
        rt_unit,
        rt_boolean,
        rt_booleankey,
        rt_integer,
        rt_double,
        rt_value,
        rt_string,
        rt_message
    }

    private Integer             id = null;
    private String              name = null;
    private String              description = null;
    private String              unit = null;
    private Map<String, String> messages = new HashMap<String, String>();

    private TestValue result;

    MapXmlResult(TestGroup parent, String key, Node nContent, Node nSchema, MapXmlFailures failures, boolean truncateStrings) throws ValidationException {
        int nItem;
        NamedNodeMap attributes;
        ResultTags type;

        // Step 1 - Process dependent properties inside root attributes
        for (Node nSNode = nSchema.getFirstChild(); nSNode != null; nSNode = nSNode.getNextSibling()) {
            if (nSNode.getNodeType() == Node.ELEMENT_NODE && nSNode.getLocalName().equalsIgnoreCase("attribute")) {
                type = getTypeTag(nSNode);
                String attrName =  getNameTag(nSNode);
                switch (type) {
                    case rt_id:
                        if (id == null) {
                            attributes = nContent.getAttributes();
                            nItem = attributes.getLength();
                            for (int i = 0; i < nItem; ++i){
                                Node node = attributes.item(i);
                                if(attrName.equals(node.getNodeName())) {
                                    try {
                                        id = Integer.parseInt(node.getTextContent());
                                    } catch (Exception e){
                                        throw new ValidationException( Messages.IncorrectIntegerForId(parent.getFullName()) );
                                    }
                                    break;
                                }
                            }
                        }
                        break;

                    case rt_name:
                        if (name == null) {
                            attributes = nContent.getAttributes();
                            nItem = attributes.getLength();
                            for (int i = 0; i < nItem; ++i){
                                Node node = attributes.item(i);
                                if(attrName.equals(node.getNodeName())) {
                                    name = node.getNodeValue();
                                    name = name.replaceAll(" ", "_");
                                    break;
                                }
                            }
                        }
                        break;

                    case rt_description:
                        if (description == null) {
                            attributes = nContent.getAttributes();
                            nItem = attributes.getLength();
                            for (int i = 0; i < nItem; ++i){
                                Node node = attributes.item(i);
                                if(attrName.equals(node.getNodeName())) {
                                    description = node.getNodeValue();
                                    if (truncateStrings && description.length() > 512) {
                                        description = description.substring(0, 512);
                                        description += "...";
                                    }
                                    break;
                                }
                            }
                        }
                        break;

                    case rt_unit:
                        if (unit == null) {
                            attributes = nContent.getAttributes();
                            nItem = attributes.getLength();
                            for (int i = 0; i < nItem; ++i) {
                                Node node = attributes.item(i);
                                if (attrName.equals(node.getNodeName())) {
                                    unit = node.getNodeValue();
                                    break;
                                }
                            }
                        }
                        break;

                    case rt_message:
                        attributes = nContent.getAttributes();
                        nItem = attributes.getLength();
                        for (int i = 0; i < nItem; ++i) {
                            Node node = attributes.item(i);
                            if (attrName.equals(node.getNodeName())) {
                                if (truncateStrings) {
                                    String message = node.getNodeValue();
                                    if (message.length() > 512) {
                                        message = message.substring(0, 512);
                                        message += "...";
                                    }
                                    messages.put(attrName, message);
                                } else {
                                    messages.put(attrName, node.getNodeValue());
                                }
                                break;
                            }
                        }
                        break;
                }
            }
        }

        // Find the inner sequence of elements
        Node nSequence = null;
        for (Node nSNode = nSchema.getFirstChild(); nSNode != null; nSNode = nSNode.getNextSibling()) {
            if (nSNode.getNodeType() == Node.ELEMENT_NODE && nSNode.getLocalName().equalsIgnoreCase("sequence")) {
                nSequence = nSNode;
                break;
            }
        }

        // Step 2 - Process dependent properties found inside inner elements
        if (nSequence != null) {
            for (Node nSNode = nSequence.getFirstChild(); nSNode != null; nSNode = nSNode.getNextSibling()) {
                if (nSNode.getNodeType() == Node.ELEMENT_NODE && nSNode.getLocalName().equalsIgnoreCase("element")) {
                    type = getTypeTag(nSNode);
                    String attrName = getNameTag(nSNode);
                    switch (type) {
                        case rt_id:
                            if (id == null) {
                                for (Node nCNode = nContent.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
                                    if (attrName.equals(nCNode.getNodeName())) {
                                        try {
                                            id = Integer.parseInt(nCNode.getTextContent());
                                        } catch (Exception e) {
                                            throw new ValidationException( Messages.IncorrectIntegerForId(parent.getFullName()) );
                                        }
                                    }
                                }
                            }
                            break;

                        case rt_name:
                            if (name == null) {
                                for (Node nCNode = nContent.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
                                    if (attrName.equals(nCNode.getNodeName())) {
                                        name = nCNode.getTextContent();
                                        name = name.replaceAll(" ", "_");
                                        break;
                                    }
                                }
                            }
                            break;

                        case rt_description:
                            if (description == null) {
                                for (Node nCNode = nContent.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
                                    if (attrName.equals(nCNode.getNodeName())) {
                                        description = nCNode.getTextContent();
                                        if (truncateStrings && description.length() > 512) {
                                            description = description.substring(0, 512);
                                            description += "...";
                                        }
                                        break;
                                    }
                                }
                            }
                            break;

                        case rt_unit:
                            if (unit == null) {
                                for (Node nCNode = nContent.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
                                    if (attrName.equals(nCNode.getNodeName())) {
                                        unit = nCNode.getTextContent();
                                        break;
                                    }
                                }
                            }
                            break;

                        case rt_message:
                            for (Node nCNode = nContent.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
                                if (attrName.equals(nCNode.getNodeName())) {
                                    if (truncateStrings) {
                                        String message = nCNode.getTextContent();
                                        if (message.length() > 512) {
                                            message = message.substring(0, 512);
                                            message += "...";
                                        }
                                        messages.put(attrName, message);
                                    } else {
                                        messages.put(attrName, nCNode.getTextContent());
                                    }
                                    break;
                                }
                            }
                            break;
                    }
                }
            }
        }

        // Step 3 - Process results found inside root attributes
        boolean resultDetected = false;
        for (Node nSNode = nSchema.getFirstChild(); nSNode != null; nSNode = nSNode.getNextSibling()) {
            if (nSNode.getNodeType() == Node.ELEMENT_NODE && nSNode.getLocalName().equalsIgnoreCase("attribute")) {
                type = getTypeTag(nSNode);
                String attrName =  getNameTag(nSNode);
                switch (type) {
                    case rt_double:
                        attributes = nContent.getAttributes();
                        nItem = attributes.getLength();
                        for (int i = 0; i < nItem; ++i) {
                            Node node = attributes.item(i);
                            if (attrName.equals(node.getNodeName())) {
                                if (name == null){
                                    name = Objects.requireNonNullElse(key, attrName);
                                }
                                try {
                                    Double value = Double.parseDouble(node.getNodeValue());
                                    DoubleValue res = new DoubleValue(parent, null, name, description, unit);
                                    res.setValue(value);
                                    result = res;
                                    resultDetected = true;
                                } catch(Exception e) {
                                    throw new ValidationException( Messages.IncorrectDoubleForDouble(parent.getFullName()) );
                                }
                                break;
                            }
                        }
                        break;

                    case rt_integer:
                        attributes = nContent.getAttributes();
                        nItem = attributes.getLength();
                        for (int i = 0; i < nItem; ++i) {
                            Node node = attributes.item(i);
                            if (attrName.equals(node.getNodeName())) {
                                if (name == null){
                                    name = Objects.requireNonNullElse(key, attrName);
                                }
                                try {
                                    Integer value = Integer.parseInt(node.getNodeValue());
                                    IntegerValue res = new IntegerValue(parent, null, name, description, unit);
                                    res.setValue(value);
                                    result = res;
                                    resultDetected = true;
                                } catch(Exception e) {
                                    throw new ValidationException( Messages.IncorrectIntegerForInteger(parent.getFullName()) );
                                }
                                break;
                            }
                        }
                        break;

                    case rt_booleankey:
                        attributes = nContent.getAttributes();
                        nItem = attributes.getLength();
                        for (int i = 0; i < nItem; ++i) {
                            Node node = attributes.item(i);
                            if (attrName.equals(node.getLocalName())) {
                                if (name == null) {
                                    name = Objects.requireNonNullElse(key, attrName);
                                }
                                StringValue res = new StringValue(parent, null, name, description, unit);
                                res.setValue(attrName);
                                if (failures.hasFailures()) {
                                    res.setFailedState(failures.isFailure(attrName, true));
                                }
                                result = res;
                                resultDetected = true;
                                break;
                            }
                        }
                        break;

                    case rt_boolean:
                        attributes = nContent.getAttributes();
                        nItem = attributes.getLength();
                        for (int i = 0; i < nItem; ++i) {
                            Node node = attributes.item(i);
                            if (attrName.equals(node.getLocalName())) {
                                if (name == null){
                                    name = Objects.requireNonNullElse(key, attrName);
                                }
                                String text = node.getNodeValue();
                                // Identify boolean value
                                if (text.equalsIgnoreCase("true") && text.equalsIgnoreCase("false")) {
                                    boolean boolValue = Boolean.parseBoolean(text);
                                    BooleanValue res = new BooleanValue(parent, null, name, description, unit);
                                    res.setValue(boolValue);
                                    if (failures.hasFailures()) {
                                        res.setFailedState(failures.isFailure(boolValue));
                                    }
                                    result = res;
                                    resultDetected = true;
                                } else {
                                    try {
                                        double dblValue = Double.parseDouble(text);
                                        DoubleValue res = new DoubleValue(parent, null, name, description, unit);
                                        res.setValue(dblValue);
                                        if (failures.hasFailures()) {
                                            res.setFailedState(failures.isFailure(dblValue));
                                        }
                                        result = res;
                                        resultDetected = true;
                                    } catch (Exception e) {
                                        StringValue res = new StringValue(parent, null, name, description, unit);
                                        res.setValue(text);
                                        if (failures.hasFailures()) {
                                            res.setFailedState(failures.isFailure(text));
                                        }
                                        result = res;
                                        resultDetected = true;
                                    }
                                }
                                break;
                            }
                        }
                        break;

                    case rt_value:
                        attributes = nContent.getAttributes();
                        nItem = attributes.getLength();
                        for (int i = 0; i < nItem; ++i) {
                            Node node = attributes.item(i);
                            if (attrName.equals(node.getNodeName())) {
                                if (name == null){
                                    name = Objects.requireNonNullElse(key, attrName);
                                }
                                String text = node.getNodeValue();
                                // Identify boolean value
                                if (text.equalsIgnoreCase("true") && text.equalsIgnoreCase("false")) {
                                    boolean boolValue = Boolean.parseBoolean(text);
                                    BooleanValue res = new BooleanValue(parent, null, name, description, unit);
                                    res.setValue(boolValue);
                                    if (failures.hasFailures()) {
                                        res.setFailedState(failures.isFailure(boolValue));
                                    }
                                    result = res;
                                    resultDetected = true;
                                } else {
                                    try {
                                        DoubleValue res = new DoubleValue(parent, null, name, description, unit);
                                        res.setValue(Double.parseDouble(text));
                                        result = res;
                                        resultDetected = true;
                                    } catch (Exception e) {
                                        StringValue res = new StringValue(parent, null, name, description, unit);
                                        res.setValue(text);
                                        if (failures.hasFailures()) {
                                            res.setFailedState(failures.isFailure(text));
                                        }
                                        result = res;
                                        resultDetected = true;
                                    }
                                }
                                break;
                            }
                        }
                        break;
                }
                if (resultDetected)
                    break;
            }
        }
        if (resultDetected) {
            result.setMessages(messages);
            return;
        }


        // Step 4 - Process results found inside the inner elements
        if (nSequence != null) {
            for (Node nSNode = nSequence.getFirstChild(); nSNode != null; nSNode = nSNode.getNextSibling()) {
                if (nSNode.getNodeType() == Node.ELEMENT_NODE && nSNode.getLocalName().equalsIgnoreCase("element")) {
                    type = getTypeTag(nSNode);
                    String attrName = getNameTag(nSNode);
                    switch (type) {
                        case rt_double:
                            for (Node nCNode = nContent.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
                                if (attrName.equals(nCNode.getLocalName())) {
                                    if (name == null) {
                                        name = Objects.requireNonNullElse(key, attrName);
                                    }
                                    try {
                                        Double value = Double.parseDouble(nCNode.getTextContent());
                                        DoubleValue res = new DoubleValue(parent, null, name, description, unit);
                                        res.setValue(value);
                                        result = res;
                                        resultDetected = true;
                                    } catch (Exception e) {
                                        throw new ValidationException( Messages.IncorrectDoubleForDouble(parent.getFullName()) );
                                    }
                                    break;
                                }
                            }
                            break;

                        case rt_integer:
                            for (Node nCNode = nContent.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
                                if (attrName.equals(nCNode.getLocalName())) {
                                    if (name == null) {
                                        name = Objects.requireNonNullElse(key, attrName);
                                    }
                                    try {
                                        Integer value = Integer.parseInt(nCNode.getTextContent());
                                        IntegerValue res = new IntegerValue(parent, null, name, description, unit);
                                        res.setValue(value);
                                        result = res;
                                        resultDetected = true;
                                    } catch (Exception e) {
                                        throw new ValidationException( Messages.IncorrectIntegerForInteger(parent.getFullName()) );
                                    }
                                    break;
                                }
                            }
                            break;

                        case rt_booleankey:
                            for (Node nCNode = nContent.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
                                if (attrName.equals(nCNode.getLocalName())) {
                                    if (name == null) {
                                        name = Objects.requireNonNullElse(key, attrName);
                                    }
                                    StringValue res = new StringValue(parent, null, name, description, unit);
                                    res.setValue(attrName);
                                    if (failures.hasFailures()) {
                                        res.setFailedState(failures.isFailure(attrName, true));
                                    }
                                    result = res;
                                    resultDetected = true;
                                }
                            }
                            break;

                        case rt_boolean:
                            for (Node nCNode = nContent.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
                                if (attrName.equals(nCNode.getLocalName())) {
                                    if (name == null) {
                                        name = Objects.requireNonNullElse(key, attrName);
                                    }
                                    String text = nCNode.getTextContent();
                                    if (text.length() == 0 ){
                                        // Key is the boolean
                                        StringValue res = new StringValue(parent, null, name, description, unit);
                                        res.setValue(attrName);
                                        if (failures.hasFailures()) {
                                            res.setFailedState(failures.isFailure(attrName, true));
                                        }
                                        result = res;
                                        resultDetected = true;
                                    } else {
                                        // Identify boolean value
                                        if (text.equalsIgnoreCase("true") && text.equalsIgnoreCase("false")) {
                                            boolean boolValue = Boolean.parseBoolean(text);
                                            BooleanValue res = new BooleanValue(parent, null, name, description, unit);
                                            res.setValue(boolValue);
                                            if (failures.hasFailures()) {
                                                res.setFailedState(failures.isFailure(boolValue));
                                            }
                                            result = res;
                                            resultDetected = true;
                                        } else {
                                            // Identify Integer/Double/String boolean value
                                            try {
                                                double dblValue = Double.parseDouble(text);
                                                DoubleValue res = new DoubleValue(parent, null, name, description, unit);
                                                res.setValue(dblValue);
                                                if (failures.hasFailures()) {
                                                    res.setFailedState(failures.isFailure(dblValue));
                                                }
                                                result = res;
                                                resultDetected = true;
                                            } catch (Exception e) {
                                                StringValue res = new StringValue(parent,null, name, description, unit);
                                                res.setValue(text);
                                                if (failures.hasFailures()) {
                                                    res.setFailedState(failures.isFailure(text));
                                                }
                                                result = res;
                                                resultDetected = true;
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                            break;

                        case rt_value:
                            for (Node nCNode = nContent.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
                                if (attrName.equals(nCNode.getLocalName())) {
                                    if (name == null) {
                                        name = Objects.requireNonNullElse(key, attrName);
                                    }
                                    String text = nCNode.getTextContent();
                                    if (text.length() == 0 ){
                                        // Key is the boolean
                                        StringValue res = new StringValue(parent, null, name, description, unit);
                                        res.setValue(attrName);
                                        if (failures.hasFailures()) {
                                            res.setFailedState(failures.isFailure(attrName, true));
                                        }
                                        result = res;
                                        resultDetected = true;
                                    } else {
                                        // Identify boolean value
                                        if (text.equalsIgnoreCase("true") && text.equalsIgnoreCase("false")) {
                                            boolean boolValue = Boolean.parseBoolean(text);
                                            BooleanValue res = new BooleanValue(parent, null, name, description, unit);
                                            res.setValue(boolValue);
                                            if (failures.hasFailures()) {
                                                res.setFailedState(failures.isFailure(boolValue));
                                            }
                                            result = res;
                                            resultDetected = true;
                                        } else {
                                            // Identify Integer/Double/String boolean value
                                            try {
                                                DoubleValue res = new DoubleValue(parent, null, name, description, unit);
                                                res.setValue(Double.parseDouble(text));
                                                result = res;
                                                resultDetected = true;
                                            } catch (Exception e) {
                                                StringValue res = new StringValue(parent, null, name, description, unit);
                                                res.setValue(text);
                                                if (failures.hasFailures()) {
                                                    res.setFailedState(failures.isFailure(text));
                                                }
                                                result = res;
                                                resultDetected = true;
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                            break;
                    }
                    if (resultDetected)
                        break;
                }
            }
            if (resultDetected) {
                result.setMessages(messages);
                return;
            }
        }

        // Step 5 - Process results found inside the root element
        if (key.equals(nContent.getNodeName())) {
            if (name == null) {
                name = key;
            }
            String text = nContent.getTextContent();
            if (text.length() == 0 ){
                // Key is the boolean
                StringValue res = new StringValue(parent, null, name, description, unit);
                res.setValue(key);
                if (failures.hasFailures()) {
                    res.setFailedState(failures.isFailure(key, true));
                }
                result = res;
                resultDetected = true;
            } else {
                // Identify boolean value
                if (text.equalsIgnoreCase("true") && text.equalsIgnoreCase("false")) {
                    boolean boolValue = Boolean.parseBoolean(text);
                    BooleanValue res = new BooleanValue(parent, null, name, description, unit);
                    res.setValue(boolValue);
                    if (failures.hasFailures()) {
                        res.setFailedState(failures.isFailure(boolValue));
                    }
                    result = res;
                    resultDetected = true;
                } else {
                    // Identify Integer/Double/String boolean value
                    try {
                        DoubleValue res = new DoubleValue(parent, null, name, description, unit);
                        res.setValue(Double.parseDouble(text));
                        result = res;
                        resultDetected = true;
                    } catch (Exception e) {
                        StringValue res = new StringValue(parent, null, name, description, unit);
                        res.setValue(text);
                        if(failures.hasFailures()) {
                            res.setFailedState(failures.isFailure(text));
                        }
                        result = res;
                        resultDetected = true;
                    }
                }
            }
        }
        if (resultDetected) {
            result.setMessages(messages);
        }
    }

    /**
     * Retrieve the value of the 'name' attribute.
     * @param nSchema XMl schema node
     * @return Enum value for Result tag
     */
    private String getNameTag(Node nSchema){

        NamedNodeMap attributes = nSchema.getAttributes();
        for (int i = 0; i < attributes.getLength(); ++i) {
            String name = attributes.item(i).getNodeName();
            if (name.equalsIgnoreCase("name")) {
                return attributes.item(i).getNodeValue();
            }
        }
        return null;
    }

    /**
     * Retrieve the value of the 'type' attribute.
     * @param nSchema XMl schema node
     * @return Enum value for Result tag
     */
    private ResultTags getTypeTag(Node nSchema){

        NamedNodeMap attributes = nSchema.getAttributes();
        for (int i = 0; i < attributes.getLength(); ++i) {
            String name = attributes.item(i).getNodeName();
            if (name.equalsIgnoreCase("type")) {
                String value = attributes.item(i).getNodeValue().toLowerCase();
                switch (value) {
                    case "jbs:id":
                        return ResultTags.rt_id;
                    case "jbs:name":
                        return ResultTags.rt_name;
                    case "jbs:description":
                        return ResultTags.rt_description;
                    case "jbs:unit":
                        return ResultTags.rt_unit;
                    case "jbs:value":
                        return ResultTags.rt_value;
                    case "jbs:boolean":
                        return ResultTags.rt_boolean;
                    case "jbs:booleankey":
                        return ResultTags.rt_booleankey;
                    case "jbs:integer":
                        return ResultTags.rt_integer;
                    case "jbs:double":
                        return ResultTags.rt_double;
                    case "jbs:string":
                        return ResultTags.rt_string;
                    case "jbs:message":
                        return ResultTags.rt_message;
                    default:
                        return ResultTags.rt_unknown;
                }
            }
        }
        return ResultTags.rt_empty;
    }

    // Getter

    public TestValue getResult() { return result; }
}
