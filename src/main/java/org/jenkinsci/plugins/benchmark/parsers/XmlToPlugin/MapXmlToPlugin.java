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

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import hudson.FilePath;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.benchmark.exceptions.ValidationException;
import org.jenkinsci.plugins.benchmark.parsers.MapperBase;
import org.jenkinsci.plugins.benchmark.results.TestGroup;
import org.jenkinsci.plugins.benchmark.results.TestValue;
import org.jenkinsci.plugins.benchmark.thresholds.Threshold;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * Parser from XML to the Jenkins plugin data construct
 *
 * @author Daniel Mercier
 * @since 6/20/2017.
 */
public class MapXmlToPlugin extends MapperBase {

    // Enumeration

    private enum GroupTags {
        gt_empty,
        gt_unknown,
        gt_result,
        gt_threshold,
        gt_parameter
    }

    // Variables

    private final Map<String, Element> complexTypes = new HashMap<String, Element>();
    private final Map<String, Element> elements = new HashMap<String, Element>();

    // Constructor

    public MapXmlToPlugin(Integer build, File content, Document schema, boolean truncateStrings) throws IOException, ValidationException {
        super(build, truncateStrings);

        Document xContent;
        try{
            xContent = getXML(content);
        } catch(Exception e) {
            throw new IOException(Messages.MapXmlToPlugin_FileFormatNotRecognisedAsXml(content.getName()));
        }
        InitiateLoading(rootGroup, xContent, schema);

    }

    public MapXmlToPlugin(Integer build, FilePath content, Document schema, boolean truncateStrings) throws IOException, ValidationException {
        super(build, truncateStrings);

        Document xContent;
        try{
            xContent = getXML(content);
        } catch(Exception e) {
            throw new IOException(Messages.MapXmlToPlugin_FileFormatNotRecognisedAsXml(content.getName()));
        }
        InitiateLoading(rootGroup, xContent, schema);
    }

    public MapXmlToPlugin(Integer build, Map<String, FilePath> content, Document schema, boolean truncateStrings, TaskListener listener) throws IOException, ValidationException {
        super(build, truncateStrings);

        listener.getLogger().println(Messages.MapXmlToPlugin_ListOfFilesDetected());

        Document xContent;
        int files_processed = 0;
        for(Map.Entry<String, FilePath> entry:content.entrySet()) {

            String relativePath = FilePathToString(entry.getValue());

            try {
                xContent = getXML(entry.getValue());
            } catch (Exception e) {
                listener.getLogger().println("   - " + Messages.MapXmlToPlugin_PrintFailedToIdentifyFile(relativePath));
                continue;
            }

            try {
                TestGroup group = new TestGroup(rootGroup, entry.getKey(), relativePath, TestValue.ClassType.ct_fileGrp);
                files.put(group.getGroupHash(), group);
                groups.put(group.getGroupHash(), group);
                rootGroup.addGroup(group);

                InitiateLoading(group, xContent, schema);
            } catch (Exception e) {
                listener.getLogger().println("   - " + Messages.MapXmlToPlugin_PrintFailedToLoadFile(relativePath));
                continue;
            }
            listener.getLogger().println("   - " + relativePath);
            files_processed++;
        }
        if (files_processed == 0) {
            throw new ValidationException(Messages.MapXmlToPlugin_NoValidFileFound());
        }
    }

    public MapXmlToPlugin(Integer build, FilePath content, String schema, boolean truncateStrings) throws IOException, ValidationException {
        super(build, truncateStrings);

        Document xSchema;
        try {
            xSchema = getXML(schema);
        } catch (Exception e) {
            throw new IOException(Messages.MapXmlToPlugin_SchemaFormatNotRecognisedAsXml());
        }

        Document xContent;
        try{
            xContent = getXML(content);
        } catch(Exception e) {
            throw new IOException(Messages.MapXmlToPlugin_FileFormatNotRecognisedAsXml(content.getName()));
        }

        InitiateLoading(rootGroup, xContent, xSchema);
    }

    public MapXmlToPlugin(Integer build, Map<String, FilePath> content, String schema, boolean truncateStrings) throws IOException, ValidationException {
        super(build, truncateStrings);

        Document xSchema;
        try {
            xSchema = getXML(schema);
        } catch (Exception e) {
            throw new IOException(Messages.MapXmlToPlugin_SchemaFormatNotRecognisedAsXml());
        }

        Document xContent;
        for(Map.Entry<String, FilePath> entry:content.entrySet()) {
            try {
                xContent = getXML(entry.getValue());
            } catch (Exception e) {
                throw new IOException(Messages.MapXmlToPlugin_FileFormatNotRecognisedAsXml(entry.getValue().getName()));
            }

            FilePath path = entry.getValue();
            String relativePath = null;
            String nextChunk = null;
            while(path != null && !path.getName().equalsIgnoreCase("workspace")) {
                if (relativePath == null) {
                    if (nextChunk != null) {
                        relativePath = nextChunk;
                    }
                } else {
                    relativePath = nextChunk + "/" + relativePath;
                }
                nextChunk = path.getName();
                path = path.getParent();
            }

            TestGroup group = new TestGroup(rootGroup, entry.getKey(), relativePath, TestValue.ClassType.ct_fileGrp);
            files.put(group.getGroupHash(), group);
            groups.put(group.getGroupHash(), group);
            rootGroup.addGroup(group);

            InitiateLoading(group, xContent, xSchema);
        }
    }


    // Functions

    /**
     * Determine if a file is a valid XML.
     * @param xmlFile Reference to XML file
     * @return Reference to XML document.
     */
    private Document getXML(File xmlFile) throws ParserConfigurationException, SAXException, IOException, InterruptedException {
        DocumentBuilder builder = createDocumentBuilder();
        return builder.parse(xmlFile);
    }

    /**
     * Determine if a file is a valid XML.
     * @param xmlFile Reference to XML file
     * @return Reference to XML document.
     */
    private Document getXML(FilePath xmlFile) throws ParserConfigurationException, SAXException, IOException, InterruptedException {
        DocumentBuilder builder = createDocumentBuilder();
        return builder.parse(xmlFile.read());
    }

    /**
     * Determine if a string is a valid XML.
     * @param xmlContent XML content from result file
     * @return Reference to XML document.
     */
    private Document getXML(String xmlContent) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilder builder = createDocumentBuilder();
        return builder.parse(xmlContent);
    }

    private DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", Boolean.TRUE );
        factory.setFeature("http://xml.org/sax/features/external-general-entities", Boolean.FALSE);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", Boolean.FALSE);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", Boolean.FALSE );
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        factory.setExpandEntityReferences(false);
        factory.setXIncludeAware(false);

        factory.setValidating(false);
        factory.setNamespaceAware(true);

        return factory.newDocumentBuilder();
    }

    /**
     * Initiate mapping of XML content with schema
     * @param dContent XML result document
     * @param dSchema XMl schema document
     * @throws ValidationException If validation error occur
     */
    private void InitiateLoading(TestGroup group, Document dContent, Document dSchema) throws ValidationException {
        if (dSchema.getDocumentElement().getLocalName().equalsIgnoreCase("schema")) {

            Element eSchema = dSchema.getDocumentElement();

            // Isolate complex types
            for (Node nSNode = eSchema.getFirstChild(); nSNode != null; nSNode = nSNode.getNextSibling()) {
                if (nSNode.getNodeType() == Node.ELEMENT_NODE && nSNode.getLocalName().equalsIgnoreCase("complextype")) {
                    String attrName = ((Element) nSNode).getAttribute("name");
                    if (attrName != null && attrName.length() > 0) {
                        complexTypes.put(attrName, (Element) nSNode);
                    }
                }
            }

            // Isolate base Element reference
            for (Node nNode = eSchema.getFirstChild(); nNode != null; nNode = nNode.getNextSibling()) {
                if (nNode.getNodeType() == Node.ELEMENT_NODE && nNode.getLocalName().equalsIgnoreCase("element")) {
                    String attrName = ((Element) nNode).getAttribute("name");
                    if (attrName != null && attrName.length() > 0) {
                        elements.put(attrName, (Element) nNode);
                    }
                }
            }

            ProcessSequence (group, "__first__", dContent, eSchema, null);
        }else {
            throw new ValidationException(Messages.MapXmlToPlugin_SchemaRootElementAndNamespaceAreIncorrect());
        }
    }

    /**
     * Process the inner sequence
     * @param parent Pointer to parent group
     * @param key Key associated with XML node from result file
     * @param nContent XML node from result file
     * @param nSchema XML node from schema file
     * @param failures List of failure criteria
     * @throws ValidationException If validation error occur
     */
    private void ProcessSequence (TestGroup parent, String key, Node nContent, Node nSchema, MapXmlFailures failures) throws ValidationException {

        // Collect failure definitions
        MapXmlFailures newFailures = new MapXmlFailures(rootGroup, nSchema, failures);

        // Process elements
        for (Node nSNode = nSchema.getFirstChild(); nSNode != null; nSNode = nSNode.getNextSibling()) {
            if (nSNode.getNodeType() == Node.ELEMENT_NODE) {
                String name = nSNode.getLocalName();
                if (name.equalsIgnoreCase("element")) {
                    ProcessElement(parent, key, nContent, nSNode, newFailures);
                }
            }
        }
    }

    /**
     * Process XML element
     *
     * @param parent Pointer to parent group
     * @param key Key associated with XML node from result file
     * @param nContent XML node from result file
     * @param nSchema XML node from schema file
     * @param failures List of failure criteria
     * @throws ValidationException If validation error occur
     */
    private void ProcessElement (TestGroup parent, String key, Node nContent, Node nSchema, MapXmlFailures failures) throws ValidationException {

        String name = getElementAttribute (nSchema, "name");
        if (name == null) return;

        GroupTags type = getElementType (nSchema);
        switch (type) {
            case gt_empty:
                for (Node nSNode = nSchema.getFirstChild(); nSNode != null; nSNode = nSNode.getNextSibling()) {
                    if (nSNode.getNodeType() == Node.ELEMENT_NODE && nSNode.getLocalName().equalsIgnoreCase("complextype")) {
                        ProcessComplexType (parent, name, nContent, nSNode, failures);
                    }
                }
                break;

            case gt_unknown: // TestGroup
                String value = getElementAttribute (nSchema, "type");
                Element nSElement = complexTypes.get(value);
                if (nSElement != null) {
                    ProcessComplexType (parent, name, nContent, nSElement, failures);
                }
                break;

            case gt_parameter:
                for (Node nSNode = nSchema.getFirstChild(); nSNode != null; nSNode = nSNode.getNextSibling()) {
                    if (nSNode.getNodeType() == Node.ELEMENT_NODE && nSNode.getLocalName().equalsIgnoreCase("complextype")) {
                        ProcessParameter (parent, name, nContent, nSNode, failures);
                    }
                }
                break;

            case gt_result:
                for (Node nSNode = nSchema.getFirstChild(); nSNode != null; nSNode = nSNode.getNextSibling()) {
                    if (nSNode.getNodeType() == Node.ELEMENT_NODE && nSNode.getLocalName().equalsIgnoreCase("complextype")) {
                        ProcessResult (parent, name, nContent, nSNode, failures);
                    }
                }
                break;

            case gt_threshold:
                for (Node nSNode = nSchema.getFirstChild(); nSNode != null; nSNode = nSNode.getNextSibling()) {
                    if (nSNode.getNodeType() == Node.ELEMENT_NODE && nSNode.getLocalName().equalsIgnoreCase("complextype")) {
                        ProcessThreshold (parent, name, nContent, nSNode, failures);
                    }
                }
                break;
        }
    }

    /**
     * Direct a complex type to its proper processing unit.
     *
     * @param parent Pointer to parent group
     * @param key Key associated with XML node from result file
     * @param nContent XML node from result file
     * @param nSchema XML node from schema file
     * @param failures List of failure criteria
     * @throws ValidationException If validation error occur
     */
    private void ProcessComplexType (TestGroup parent, String key, Node nContent, Node nSchema, MapXmlFailures failures) throws ValidationException {
        GroupTags typeCT = getElementType(nSchema);
        switch (typeCT) {
            case gt_parameter:
                ProcessParameter (parent, key, nContent, nSchema, failures);
                break;
            case gt_result:
                ProcessResult (parent, key, nContent, nSchema, failures);
                break;
            case gt_threshold:
                ProcessThreshold (parent, key, nContent, nSchema, failures);
                break;
            default:
                ProcessGroup (parent, key, nContent, nSchema, failures);
        }
    }

    /**
     * Process threshold information
     *
     * @param parent Pointer to parent group
     * @param key Key associated with XML node from result file
     * @param nContent XML node from result file
     * @param nSchema XML node from schema file
     * @param failures List of failure criteria
     * @throws ValidationException If validation error occur
     */
    private void ProcessThreshold (TestGroup parent, String key, Node nContent, Node nSchema, MapXmlFailures failures) throws ValidationException {

        for (Node nCNode = nContent.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
            if (nCNode.getNodeType() == Node.ELEMENT_NODE && nCNode.getNodeName().equals(key)) {

                MapXmlThreshold content = new MapXmlThreshold(parent, key, nCNode, nSchema);
                Threshold threshold = content.getThreshold();
                if (threshold != null) {
                    parent.addThreshold(threshold);
                    checkThresholdType(threshold);
                }
            }
        }
    }

    /**
     * Process parameter information
     *
     * @param parent Pointer to parent group
     * @param key Key associated with XML node from result file
     * @param nContent XML node from result file
     * @param nSchema XML node from schema file (complex type)
     * @param failures List of failure criteria
     * @throws ValidationException If validation error occur
     */
    private void ProcessParameter (TestGroup parent, String key, Node nContent, Node nSchema, MapXmlFailures failures) throws ValidationException {

        for (Node nCNode = nContent.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
            if (nCNode.getNodeType() == Node.ELEMENT_NODE && nCNode.getNodeName().equals(key)) {

                MapXmlParameter content = new MapXmlParameter(parent, key, nCNode, nSchema, failures, truncateStrings);
                TestValue parameter = content.getParameter();
                if (parameter != null) {
                    parent.addGroup(parameter);
                    groups.put(parameter.getGroupHash(), parameter);
                    parameters.put(parameter.getGroupHash(), parameter);
                }
            }
        }
    }

    /**
     * Process result information
     *
     * @param parent Pointer to parent group
     * @param key Key associated with XML node from result file
     * @param nContent XML node from result file
     * @param nSchema XML node from schema file (complex type)
     * @param failures List of failure criteria
     * @throws ValidationException If validation error occur
     */
    private void ProcessResult (TestGroup parent, String key, Node nContent, Node nSchema, MapXmlFailures failures) throws ValidationException {

        for (Node nCNode = nContent.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
            if (nCNode.getNodeType() == Node.ELEMENT_NODE && nCNode.getNodeName().equals(key)) {

                MapXmlResult content = new MapXmlResult(parent, key, nCNode, nSchema, failures, truncateStrings);
                TestValue result = content.getResult();
                if (result != null) {
                    parent.addGroup(result);
                    checkResult(result);

                    groups.put(result.getGroupHash(), result);
                    results.put(result.getGroupHash(), result);

                    for (Node nSNode = nSchema.getFirstChild(); nSNode != null; nSNode = nSNode.getNextSibling()) {
                        if (nSNode.getNodeType() == Node.ELEMENT_NODE && nSNode.getLocalName().equalsIgnoreCase("sequence")) {
                            ProcessSequence(result, key, nCNode, nSNode, failures);
                        }
                    }
                }
            }
        }
    }

    /**
     * Process group of data in XML element
     *
     * @param parent Pointer to parent group
     * @param key Key associated with XML node from result file
     * @param nContent XML node from result file
     * @param nSchema XML node from schema file (complexType)
     * @param failures List of failure criteria
     * @throws ValidationException If validation error occur
     */
    private void ProcessGroup (TestGroup parent, String key, Node nContent, Node nSchema, MapXmlFailures failures) throws ValidationException {

        for (Node nCNode = nContent.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
            if (nCNode.getNodeType() == Node.ELEMENT_NODE && nCNode.getNodeName().equals(key)) {

                MapXmlGroup content = new MapXmlGroup(parent, key, nCNode, nSchema, failures, truncateStrings);
                TestGroup group = content.getGroup();
                if (group != null) {
                    parent.addGroup(group);
                    groups.put(group.getGroupHash(), group);

                    for (Node nSNode = nSchema.getFirstChild(); nSNode != null; nSNode = nSNode.getNextSibling()) {
                        if (nSNode.getNodeType() == Node.ELEMENT_NODE && nSNode.getLocalName().equalsIgnoreCase("sequence")) {
                            ProcessSequence(group, key, nCNode, nSNode, failures);
                        }
                    }

                    // Detect if array of parameters
                    group.isParameterGrp();

                    // Detect if array of thresholds
                    group.isThresholdGrp();
                }
            }
        }
    }

    /**
     * Convert FilePath to String
     * @param path Original file path.
     * @return String equivalent relative to the workspace.
     */
    private String FilePathToString(FilePath path){
        String relativePath = null;
        String nextChunk = null;
        while(path != null && !path.getName().equalsIgnoreCase("workspace")) {
            if (relativePath == null) {
                if (nextChunk != null) {
                    relativePath = nextChunk;
                }
            } else {
                relativePath = nextChunk + "/" + relativePath;
            }
            nextChunk = path.getName();
            path = path.getParent();
        }
        if (nextChunk != null) {
            relativePath = nextChunk + "/" + relativePath;
        }
        return relativePath;
    }

    /**
     * Get schema element type
     * @param nSchema XML node from schema file
     * @return group tag
     */
    private GroupTags getElementType (Node nSchema) {
        NamedNodeMap attributes = nSchema.getAttributes();
        for (int i = 0; i < attributes.getLength(); ++i) {
            String name = attributes.item(i).getNodeName();
            if (name.equalsIgnoreCase("type")) {
                String value = attributes.item(i).getNodeValue().toLowerCase();
                switch (value) {
                    case "jbs:result":
                        return GroupTags.gt_result;
                    case "jbs:threshold":
                        return GroupTags.gt_threshold;
                    case "jbs:parameter":
                        return GroupTags.gt_parameter;
                    default:
                        return GroupTags.gt_unknown;
                }
            }
        }
        return GroupTags.gt_empty;
    }

    /**
     * Get schema element attributes
     *
     * @param nSchema XML node from schema file
     * @param key Key associated with XML node from result file
     * @return list of standard attributes
     */
    private String getElementAttribute (Node nSchema, String key) {
        NamedNodeMap attributes = nSchema.getAttributes();
        for (int i = 0; i < attributes.getLength(); ++i) {
            String name = attributes.item(i).getNodeName();
            if (name.equalsIgnoreCase(key)) {
                return attributes.item(i).getNodeValue();
            }
        }
        return null;
    }
}
