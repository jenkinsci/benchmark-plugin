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
import org.jenkinsci.plugins.benchmark.results.TestGroup;
import org.jenkinsci.plugins.benchmark.thresholds.*;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Map Parameter XML schema/content data to Jenkins plugin data construct
 *
 * @author Daniel Mercier
 * @since 6/20/2017.
 */
public class MapXmlThreshold {
    /**
     * Enumeration of Threshold tags
     */
    enum ThresholdTags {
        tt_empty,
        tt_unknown,
        tt_method,
        tt_minimum,
        tt_maximum,
        tt_delta,
        tt_percentage,
        tt_name,
        tt_description,
        tt_ignoreNegativeDeltas
    }

    // Variables

    private Double minimum;
    private Double maximum;
    private Double delta;
    private Double percentage;
    private Boolean ignoreNegativeDeltas;

    private Threshold threshold = null;

    MapXmlThreshold(TestGroup parent, String key, Node nContent, Node nSchema) throws ValidationException {
        int nItem;
        NamedNodeMap attributes;
        ThresholdTags type;

        // Step 1 - Process dependent properties inside root attributes
        for (Node nSNode = nSchema.getFirstChild(); nSNode != null; nSNode = nSNode.getNextSibling()) {
            if (nSNode.getNodeType() == Node.ELEMENT_NODE && nSNode.getLocalName().equalsIgnoreCase("attribute")) {
                type = getTypeTag(nSNode);
                String attrName =  getNameTag(nSNode);
                switch (type) {
                    case tt_minimum:
                        if (minimum == null) {
                            attributes = nContent.getAttributes();
                            nItem = attributes.getLength();
                            for (int i = 0; i < nItem; ++i){
                                Node node = attributes.item(i);
                                if(attrName.equals(node.getNodeName())) {
                                    try {
                                        minimum = Double.parseDouble(node.getTextContent());
                                    } catch (Exception e){
                                        throw new ValidationException( Messages.IncorrectDoubleForMinimum(parent.getFullName()) );
                                    }
                                    break;
                                }
                            }
                        }
                        break;

                    case tt_maximum:
                        if (maximum == null) {
                            attributes = nContent.getAttributes();
                            nItem = attributes.getLength();
                            for (int i = 0; i < nItem; ++i){
                                Node node = attributes.item(i);
                                if(attrName.equals(node.getNodeName())) {
                                    try {
                                        maximum = Double.parseDouble(node.getTextContent());
                                    } catch (Exception e){
                                        throw new ValidationException( Messages.IncorrectDoubleForMaximum(parent.getFullName()) );
                                    }
                                    break;
                                }
                            }
                        }
                        break;

                    case tt_delta:
                        if (delta == null) {
                            attributes = nContent.getAttributes();
                            nItem = attributes.getLength();
                            for (int i = 0; i < nItem; ++i){
                                Node node = attributes.item(i);
                                if(attrName.equals(node.getNodeName())) {
                                    try {
                                        delta = Double.parseDouble(node.getTextContent());
                                    } catch (Exception e){
                                        throw new ValidationException( Messages.IncorrectDoubleForDelta(parent.getFullName()) );
                                    }
                                    break;
                                }
                            }
                        }
                        break;

                    case tt_percentage:
                        if (delta == null) {
                            attributes = nContent.getAttributes();
                            nItem = attributes.getLength();
                            for (int i = 0; i < nItem; ++i){
                                Node node = attributes.item(i);
                                if(attrName.equals(node.getNodeName())) {
                                    try {
                                        percentage = Double.parseDouble(node.getTextContent());
                                    } catch (Exception e){
                                        throw new ValidationException( Messages.IncorrectDoubleForPercentage(parent.getFullName()) );
                                    }
                                    break;
                                }
                            }
                        }
                        break;

                    case tt_ignoreNegativeDeltas:
                        if (ignoreNegativeDeltas == null){
                            attributes = nContent.getAttributes();
                            nItem = attributes.getLength();
                            for (int i = 0; i < nItem; ++i){
                                Node node = attributes.item(i);
                                if(attrName.equals(node.getNodeName())) {
                                    try {
                                        ignoreNegativeDeltas = Boolean.parseBoolean(node.getTextContent());
                                    } catch (Exception e){
                                        throw new ValidationException( Messages.IncorrectBooleanForIgnoreNegativeDeltas(parent.getFullName()) );
                                    }
                                    break;
                                }
                            }
                        }
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
                        case tt_minimum:
                            if (minimum == null) {
                                for (Node nCNode = nContent.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
                                    if (attrName.equals(nCNode.getNodeName())) {
                                        try {
                                            minimum = Double.parseDouble(nCNode.getTextContent());
                                        } catch (Exception e){
                                            throw new ValidationException( Messages.IncorrectDoubleForMinimum(parent.getFullName()) );
                                        }
                                        break;
                                    }
                                }
                            }
                            break;

                        case tt_maximum:
                            if (maximum == null) {
                                for (Node nCNode = nContent.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
                                    if (attrName.equals(nCNode.getNodeName())) {
                                        try {
                                            maximum = Double.parseDouble(nCNode.getTextContent());
                                        } catch (Exception e){
                                            throw new ValidationException( Messages.IncorrectDoubleForMaximum(parent.getFullName()) );
                                        }
                                        break;
                                    }
                                }
                            }
                            break;

                        case tt_delta:
                            if (delta == null) {
                                for (Node nCNode = nContent.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
                                    if (attrName.equals(nCNode.getNodeName())) {
                                        try {
                                            delta = Double.parseDouble(nCNode.getTextContent());
                                        } catch (Exception e){
                                            throw new ValidationException( Messages.IncorrectDoubleForDelta(parent.getFullName()) );
                                        }
                                        break;
                                    }
                                }
                            }
                            break;

                        case tt_percentage:
                            if (delta == null) {
                                for (Node nCNode = nContent.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
                                    if (attrName.equals(nCNode.getNodeName())) {
                                        try {
                                            percentage = Double.parseDouble(nCNode.getTextContent());
                                        } catch (Exception e){
                                            throw new ValidationException( Messages.IncorrectDoubleForPercentage(parent.getFullName()) );
                                        }
                                        break;
                                    }
                                }
                            }
                            break;

                        case tt_ignoreNegativeDeltas:
                            if (ignoreNegativeDeltas == null) {
                                for (Node nCNode = nContent.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
                                    if (attrName.equals(nCNode.getNodeName())) {
                                        try {
                                            ignoreNegativeDeltas = Boolean.parseBoolean(nCNode.getTextContent());
                                        } catch (Exception e){
                                            throw new ValidationException( Messages.IncorrectBooleanForIgnoreNegativeDeltas(parent.getFullName()) );
                                        }
                                        break;
                                    }
                                }
                            }
                            break;
                    }
                }
            }
        }


        // Step 3 - Process results found inside root attributes
        boolean thresholdDetected = false;
        for (Node nSNode = nSchema.getFirstChild(); nSNode != null; nSNode = nSNode.getNextSibling()) {
            if (nSNode.getNodeType() == Node.ELEMENT_NODE && nSNode.getLocalName().equalsIgnoreCase("attribute")) {
                type = getTypeTag(nSNode);
                String attrName =  getNameTag(nSNode);
                switch (type) {
                    case tt_method:
                        attributes = nContent.getAttributes();
                        nItem = attributes.getLength();
                        for (int i = 0; i < nItem; ++i) {
                            Node node = attributes.item(i);
                            if (attrName.equals(node.getNodeName())) {
                                String method = node.getNodeValue().toLowerCase();
                                if (method.equals("absolute")){
                                    AbsoluteThreshold thres = new AbsoluteThreshold(minimum, maximum);
                                    threshold = thres;
                                    thresholdDetected = true;
                                } else if (method.equals("percentage")) {
                                    PercentageThreshold thres = new PercentageThreshold(percentage);
                                    threshold = thres;
                                    thresholdDetected = true;
                                } else if (method.equals("delta")) {
                                    DeltaThreshold thres = new DeltaThreshold(delta, ignoreNegativeDeltas);
                                    threshold = thres;
                                    thresholdDetected = true;
                                } else if (method.equals("percentageaverage")) {
                                    PercentageAverageThreshold thres = new PercentageAverageThreshold(percentage);
                                    threshold = thres;
                                    thresholdDetected = true;
                                } else if (method.equals("deltaaverage")){
                                    DeltaAverageThreshold thres = new DeltaAverageThreshold(delta, ignoreNegativeDeltas);
                                    threshold = thres;
                                    thresholdDetected = true;
                                }
                                break;
                            }
                        }
                        break;
                }
            }
        }

        if (thresholdDetected) {
            return;
        }

        // Step 4 - Process results found inside the inner elements
        if (nSequence != null) {
            for (Node nSNode = nSequence.getFirstChild(); nSNode != null; nSNode = nSNode.getNextSibling()) {
                if (nSNode.getNodeType() == Node.ELEMENT_NODE && nSNode.getLocalName().equalsIgnoreCase("element")) {
                    type = getTypeTag(nSNode);
                    String attrName = getNameTag(nSNode);
                    switch (type) {
                        case tt_method:
                            for (Node nCNode = nContent.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
                                if (attrName.equals(nCNode.getLocalName())) {
                                    String method = nCNode.getTextContent().toLowerCase();
                                    if (method.equals("absolute")){
                                        AbsoluteThreshold thres = new AbsoluteThreshold(minimum, maximum);
                                        threshold = thres;
                                        thresholdDetected = true;
                                    } else if (method.equals("percentage")) {
                                        PercentageThreshold thres = new PercentageThreshold(percentage);
                                        threshold = thres;
                                        thresholdDetected = true;
                                    } else if (method.equals("delta")) {
                                        DeltaThreshold thres = new DeltaThreshold(delta, ignoreNegativeDeltas);
                                        threshold = thres;
                                        thresholdDetected = true;
                                    } else if (method.equals("percentageaverage")) {
                                        PercentageAverageThreshold thres = new PercentageAverageThreshold(percentage);
                                        threshold = thres;
                                        thresholdDetected = true;
                                    } else if (method.equals("deltaaverage")){
                                        DeltaAverageThreshold thres = new DeltaAverageThreshold(delta, ignoreNegativeDeltas);
                                        threshold = thres;
                                        thresholdDetected = true;
                                    }
                                    break;
                                }
                            }
                            break;
                    }
                }
            }
        }

        if (thresholdDetected) {
            return;
        }

        // Step 5 - Process results found inside the root element
        if (key.equals(nContent.getNodeName())) {
            String method = nContent.getTextContent().toLowerCase();
            if (method.equals("absolute")){
                AbsoluteThreshold thres = new AbsoluteThreshold(minimum, maximum);
                threshold = thres;
            } else if (method.equals("percentage")) {
                PercentageThreshold thres = new PercentageThreshold(percentage);
                threshold = thres;
            } else if (method.equals("delta")) {
                DeltaThreshold thres = new DeltaThreshold(delta, ignoreNegativeDeltas);
                threshold = thres;
            } else if (method.equals("percentageaverage")) {
                PercentageAverageThreshold thres = new PercentageAverageThreshold(percentage);
                threshold = thres;
            } else if (method.equals("deltaaverage")){
                DeltaAverageThreshold thres = new DeltaAverageThreshold(delta, ignoreNegativeDeltas);
                threshold = thres;
            }
        }
    }

    // Functions

    /**
     * Retrieve the value of the 'name' attribute.
     * @param nSchema XML schema node
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
     * Retrieve the type of threshold tag associated with 'type'
     * @param nSchema XML node from schema file
     * @return group tag
     */
    private ThresholdTags getTypeTag (Node nSchema) {
        NamedNodeMap attributes = nSchema.getAttributes();
        for (int i = 0; i < attributes.getLength(); ++i) {
            String name = attributes.item(i).getNodeName();
            if (name.equalsIgnoreCase("type")) {
                String value = attributes.item(i).getNodeValue().toLowerCase();
                if (value.equals("jbs:method")){
                    return ThresholdTags.tt_method;
                } else if (value.equals("jbs:minimum")) {
                    return ThresholdTags.tt_minimum;
                } else if (value.equals("jbs:maximum")) {
                    return ThresholdTags.tt_maximum;
                } else if (value.equals("jbs:delta")) {
                    return ThresholdTags.tt_delta;
                } else if (value.equals("jbs:percentage")) {
                    return ThresholdTags.tt_percentage;
                } else if (value.equals("jbs:ignoreNegativeDeltas")) {
                    return ThresholdTags.tt_ignoreNegativeDeltas;
                } else {
                    return ThresholdTags.tt_unknown;
                }
            }
        }
        return ThresholdTags.tt_empty;
    }

    // Getter

    public Threshold getThreshold() { return threshold; }
}
