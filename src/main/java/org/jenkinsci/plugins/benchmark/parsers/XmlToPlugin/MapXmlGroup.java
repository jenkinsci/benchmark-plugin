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

import org.jenkinsci.plugins.benchmark.results.TestGroup;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Map Group XML schema/content data to Jenkins plugin data construct
 *
 * @author Daniel Mercier
 * @since 6/20/2017.
 */
public class MapXmlGroup {

    /**
     * Enumeration for Group tags
     */
    private enum GroupTags {
        gt_empty,
        gt_unknown,
        gt_name,
        gt_description
    }

    // Variables

    private String name = null;
    private String description = null;

    private TestGroup group = null;


    MapXmlGroup(TestGroup parent, String key, Node nContent, Node nSchema, MapXmlFailures failures, boolean truncateStrings) {
        int nItem;
        NamedNodeMap attributes;
        GroupTags type;

        // Step 1 - Process dependent properties inside root attributes
        for (Node nSNode = nSchema.getFirstChild(); nSNode != null; nSNode = nSNode.getNextSibling()) {
            if (nSNode.getNodeType() == Node.ELEMENT_NODE && nSNode.getLocalName().equalsIgnoreCase("attribute")) {
                type = getTypeTag(nSNode);
                String attrName =  getNameTag(nSNode);
                switch (type) {
                    case gt_name:
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

                    case gt_description:
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

        // Step 3 - Process dependent properties found inside inner elements
        if (nSequence != null) {
            for (Node nSNode = nSequence.getFirstChild(); nSNode != null; nSNode = nSNode.getNextSibling()) {
                if (nSNode.getNodeType() == Node.ELEMENT_NODE && nSNode.getLocalName().equalsIgnoreCase("element")) {
                    type = getTypeTag(nSNode);
                    String attrName = getNameTag(nSNode);
                    switch (type) {
                        case gt_name:
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

                        case gt_description:
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
                    }
                }
            }
        }

        if (name == null){
            name = key;
        }
        group = new TestGroup(parent, name, description);
    }

    // Functions

    /**
     * Retrieve the value of the 'name' attribute.
     *
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
     * Retrieve the type of Result tag associate to 'type'
     *
     * @param nSchema XML schema node
     * @return Enum value for Result tag
     */
    private GroupTags getTypeTag(Node nSchema){

        NamedNodeMap attributes = nSchema.getAttributes();
        for (int i = 0; i < attributes.getLength(); ++i) {
            String name = attributes.item(i).getNodeName();
            if (name.equalsIgnoreCase("type")) {
                String value = attributes.item(i).getNodeValue().toLowerCase();
                if (value.equals("jbs:name")) {
                    return GroupTags.gt_name;
                } else if (value.equals("jbs:description")) {
                    return GroupTags.gt_description;
                } else {
                    return GroupTags.gt_unknown;
                }
            }
        }
        return GroupTags.gt_empty;
    }

    // Getter

    public TestGroup getGroup() { return group; }
}
