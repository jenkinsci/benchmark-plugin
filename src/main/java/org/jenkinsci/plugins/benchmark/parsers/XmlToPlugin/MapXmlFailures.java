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
package org.jenkinsci.plugins.benchmark.parsers.XmlToPlugin;

import org.jenkinsci.plugins.benchmark.exceptions.ValidationException;
import org.jenkinsci.plugins.benchmark.results.TestFailure;
import org.jenkinsci.plugins.benchmark.results.TestGroup;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Map Failure XML schema data to Jenkins plugin data construct
 *
 * @author Daniel Mercier
 * @since 6/20/2017.
 */
public class MapXmlFailures {

    /**
     * Enumeration for Failure tags
     */
    private enum FailureTags {
        ft_empty,
        ft_unknown,
        ft_boolean,
        ft_value,
        ft_string,
        ft_element
    }

    // Variables

    private List<TestFailure> failures;

    // Constructor

    MapXmlFailures(TestGroup parent, Node nSchema) throws ValidationException {
        failures = new ArrayList<TestFailure>();
        LoadFailures(parent, nSchema);
    }

    MapXmlFailures(TestGroup parent, Node nSchema, MapXmlFailures oFailures) throws ValidationException {
        failures = new ArrayList<TestFailure>();
        LoadFailures(parent, nSchema);
        if (oFailures != null) {
            failures.addAll(oFailures.getFailures());
        }
    }

    // Functions

    /**
     * Load all the failures detected inside the passed JsonElement
     *
     * @param parent Pointer to parent group
     * @param nSchema Schema XML node
     */
    private void LoadFailures( TestGroup parent,  Node nSchema ) throws ValidationException {
        for (Node nNode = nSchema.getFirstChild(); nNode != null; nNode = nNode.getNextSibling()) {
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                if (nNode.getLocalName().equalsIgnoreCase("failures")) {
                    for (Node nSubNode = nNode.getFirstChild(); nSubNode != null; nSubNode = nSubNode.getNextSibling()) {
                        if (nSubNode.getNodeType() == Node.ELEMENT_NODE && nSubNode.getLocalName().equals("failure")) {
                            ProcessFailure(parent, nSubNode);
                        }
                    }
                } else if (nNode.getLocalName().equalsIgnoreCase("failure")) {
                    ProcessFailure(parent, nNode);
                }
            }
        }
    }

    private void ProcessFailure ( TestGroup parent, Node nFailure ) throws ValidationException {
        FailureTags type = getElementType (nFailure);
        switch (type) {
            case ft_boolean:
                String text = nFailure.getTextContent();
                if (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false")) {
                    failures.add(new TestFailure(Boolean.parseBoolean(text)));
                }else{
                    throw new ValidationException( Messages.MapXmlFailures_BooleanFailureOnlyTrueOrFalse() );
                }
                break;
            case ft_string:
                failures.add(new TestFailure(nFailure.getTextContent()));
                break;
            case ft_element:
                failures.add(new TestFailure(nFailure.getTextContent(), true));
                break;
            case ft_value:
                String text_2 = nFailure.getTextContent();
                try {
                    Double value = Double.parseDouble(text_2);
                    String compare = getElementAttribute(nFailure, "compare");
                    if (compare != null){
                        try {
                            failures.add(new TestFailure(value, compare));
                        } catch (ValidationException e) {
                            throw new ValidationException( Messages.MapXmlFailures_CompareIsNotRecogmizedAsType(compare, parent.getFullName()) );
                        }
                    }
                } catch (NumberFormatException e) {
                    throw new ValidationException( Messages.MapXmlFailures_TextCouldNotBeParseToNumber(text_2, parent.getFullName()) );
                }
                break;
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

    /**
     * Get schema element attributes
     * @param nSchema XML node from schema file
     * @return list of standard attributes
     */
    private String getElementAttribute (Node nSchema, String name) {
        NamedNodeMap attributes = nSchema.getAttributes();
        for (int i = 0; i < attributes.getLength(); ++i) {
            String key = attributes.item(i).getNodeName();
            if (key.equalsIgnoreCase(name)) {
                return attributes.item(i).getNodeValue();
            }
        }
        return null;
    }

    /**
     * Retrieve the type of parameter tag associated with 'type'
     * @param nSchema XML node from schema file
     * @return group tag
     */
    private FailureTags getElementType (Node nSchema) {
        NamedNodeMap attributes = nSchema.getAttributes();
        for (int i = 0; i < attributes.getLength(); ++i) {
            String name = attributes.item(i).getNodeName();
            if (name.equalsIgnoreCase("type")) {
                String value = attributes.item(i).getNodeValue().toLowerCase();
                switch (value) {
                    case "jbs:boolean":
                        return FailureTags.ft_boolean;
                    case "jbs:element":
                        return FailureTags.ft_element;
                    case "jbs:string":
                        return FailureTags.ft_string;
                    case "jbs:value":
                        return FailureTags.ft_value;
                    default:
                        return FailureTags.ft_unknown;
                }
            }
        }
        return FailureTags.ft_empty;
    }

    // Getter

    public int size(){ return failures.size(); }
    public List<TestFailure> getFailures(){ return failures; }
    public boolean hasFailures() { return (failures.size() > 0); }
}
