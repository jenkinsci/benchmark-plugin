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
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package org.jenkinsci.plugins.benchmark.parsers;

import org.jenkinsci.plugins.benchmark.exceptions.ValidationException;
import org.jenkinsci.plugins.benchmark.results.*;
import org.jenkinsci.plugins.benchmark.utilities.FilePathGroup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parse the Jenkins Unit Test report
 *
 * @author Daniel Mercier
 * @since 6/20/2017.
 */
public class jUnitJenkins extends MapperBase {

    // Variables

    // Constructors

    public jUnitJenkins(Integer build, boolean truncateStrings) throws IOException, ValidationException {
        super(build, truncateStrings);
    }

    // Functions

    public boolean importFromFile(Integer build, String inputFile) throws IOException, InterruptedException {
        File oFile = new File(inputFile);
        if (oFile.exists()) {
            this.importFromFile(build, oFile);
            return true;
        }
        return false;
    }

    /**
     * Process the Jenkins jUnit file
     * @param build Build number
     * @param filename File name
     * @throws IOException I/O exception
     * @throws InterruptedException Interrupted exception
     */
    public void importFromFile(Integer build, File filename) throws IOException, InterruptedException {

        this.builds.add(build);

        Document document;
        try{
            document = getXML(filename);
        } catch(Exception e) {
            throw new IOException(Messages.jUnitJenkins_FileFormatNotRecognizedAsXml(filename.getName()));
        }

        Element element = document.getDocumentElement();

        // Plugin information
        StringValue plugin = null;
        String text = element.getAttribute("plugin");
        if (text != null && !text.isEmpty()) {
            int hash = "plugin".hashCode();
            plugin = (StringValue)parameters.get(hash);
            if (plugin == null) {
                plugin = new StringValue(rootGroup, null,"plugin_version", null,  TestValue.ClassType.ct_parameter);
                parameters.put(hash, plugin);
                groups.put(hash, plugin);
                rootGroup.addGroup(plugin);
            }
            plugin.setValue(build, text);
        }


        DoubleValue total_duration = null;
        BooleanValue longStdio = null;
        for (Node nCNode = element.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
            if (nCNode.getNodeType() == Node.ELEMENT_NODE) {
                String nodeText = nCNode.getNodeName();
                if (nodeText.equals("duration")) {
                    // Total duration
                    text = nCNode.getTextContent();
                    if (text != null && !text.isEmpty()) {
                        Double duration = Double.parseDouble(text);
                        if (duration > 0) {
                            int hash = "duration".hashCode();
                            total_duration = (DoubleValue) parameters.get(hash);
                            if (total_duration == null) {
                                total_duration = new DoubleValue(rootGroup, null, "all_tests_duration", TestValue.ClassType.ct_parameter);
                                parameters.put(hash, total_duration);
                                groups.put(hash, total_duration);
                                rootGroup.addGroup(total_duration);
                            }
                            total_duration.setValue(build, duration);
                        }
                    }
                } else if (nodeText.equals("keepLongStdio")) {
                    // If overall keep long string
                    text = nCNode.getTextContent();
                    if (text != null && !text.isEmpty()) {
                        Boolean keepLongStdio = Boolean.parseBoolean(text);
                        if (keepLongStdio != null) {
                            int hash = "keepLongStdio".hashCode();
                            longStdio = (BooleanValue) parameters.get(hash);
                            if (longStdio == null) {
                                longStdio = new BooleanValue(rootGroup, null, "keep_long_stdio", TestValue.ClassType.ct_parameter);
                                parameters.put(hash, longStdio);
                                groups.put(hash, longStdio);
                                rootGroup.addGroup(longStdio);
                            }
                            longStdio.setValue(build, keepLongStdio);
                        }
                    }
                }
            }
        }

        for (Node nCNode = element.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
            if (nCNode.getNodeType() == Node.ELEMENT_NODE){
                String nodeText = nCNode.getNodeName();
                if (nCNode.getNodeName().equals("suites")) {
                    ProcessXMLSuites(rootGroup, build, nCNode, plugin, total_duration, longStdio);
                }
            }
        }
    }

    /**
     * Process list of files and their result content
     * @param parent Root group
     * @param build Build number
     * @param xNode XML content
     * @param plugin Plugin parameter
     * @param total_duration parameter
     * @param keepLongStdio Keep long string messages
     * @throws IOException I/O Exception
     * @throws InterruptedException Interrupted exception
     */
    private void ProcessXMLSuites(TestGroup parent, Integer build, Node xNode, StringValue plugin, DoubleValue total_duration, BooleanValue keepLongStdio) throws IOException, InterruptedException{
        int nFiles = 0;
        List<FilePathGroup> filenames = new ArrayList<FilePathGroup>();
        for (Node nCNode = xNode.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
            if(nCNode.getNodeName().equals("suite")) {
                for (Node nCNode2 = nCNode.getFirstChild(); nCNode2 != null; nCNode2 = nCNode2.getNextSibling()) {
                    if (nCNode2.getNodeName().equals("file")) {
                        String text = nCNode2.getTextContent();
                        if (text != null && !text.isEmpty()) {
                            filenames.add(new FilePathGroup(text));
                            nFiles++;
                        }
                        break;
                    }
                }
            }
        }

        if (filenames.size() != nFiles) {
            throw new IOException(Messages.jUnitJenkins_WrongNumberOfFiles());
        }

        if (nFiles > 1) {
            RecurseNames(false, 0, "", filenames);

            Node nCNode = xNode.getFirstChild();
            for (FilePathGroup file : filenames) {
                if (nCNode.getNodeName().equals("suite")) {
                    ProcessXMLSuite(false, file, parent, build, nCNode, plugin, total_duration, keepLongStdio);
                    nCNode = nCNode.getNextSibling();
                }
            }

        } else {

            for (Node nCNode = xNode.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
                if(nCNode.getNodeName().equals("suite")) {
                    ProcessXMLSuite(true, null, parent, build, nCNode, plugin, total_duration, keepLongStdio);
                }
            }

        }
    }

    /**
     * Given a list of full file paths, extra for each the relative path
     * Recursive function.
     * @param odd Whether this section
     * @param pos Current position being processed
     * @param shortName Identified shot name
     * @param names List of file paths
     */
    public static void RecurseNames(boolean odd, int pos, String shortName, List<FilePathGroup> names){

        // Determine max size
        int maxSize = 0;
        for (FilePathGroup name : names){
            if (maxSize < name.getNumberOfSegments()) {
                maxSize = name.getNumberOfSegments();
            }
        }

        if (pos < maxSize || names.size() == 0 ) {

            String base = names.get(0).getPathSegment(pos);

            // Identify similars in the current layer
            List<FilePathGroup> matchList = new ArrayList<FilePathGroup>();
            List<FilePathGroup> oddList = new ArrayList<FilePathGroup>();
            for (FilePathGroup name : names) {
                if (name.getPathSegment(pos).equals(base)) {
                    matchList.add(name);
                } else {
                    oddList.add(name);
                }
            }
            if (oddList.size() > 0) {
                if (shortName != null && shortName.length() == 0) {
                    RecurseNames(false, pos + 1, base, matchList);
                } else {
                    RecurseNames(false, pos + 1, shortName + File.separator + base, matchList);
                }
                RecurseNames(true, pos, shortName, oddList);
            } else {
                if(odd) {
                    if (shortName != null && shortName.length() == 0) {
                        RecurseNames(false, pos + 1, base, matchList);
                    } else {
                        RecurseNames(false, pos + 1, shortName + File.separator + base, matchList);
                    }
                } else {
                    RecurseNames(false, pos + 1, shortName, matchList);
                }
            }
        } else {
            for (FilePathGroup name: names) {
                name.setRelativepath(shortName);
            }
        }
    }

    /**
     * Process file and its result content
     * @param parent Root group
     * @param build Build number
     * @param xNode XML content
     * @param plugin Plugin parameter
     * @param total_duration parameter
     * @param keepLongStdio Keep long string messages
     */
    private void ProcessXMLSuite(boolean singleFile, FilePathGroup file, TestGroup parent, Integer build, Node xNode, StringValue plugin, DoubleValue total_duration, BooleanValue keepLongStdio){

        Double      _duration = null;


        // Load parameters & file attribute
        for (Node nCNode = xNode.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
            String nodeText = nCNode.getNodeName();
            if (nodeText.equals("duration")) {
                String text = nCNode.getTextContent();
                if (text != null && !text.isEmpty()) {
                    _duration = Double.parseDouble(text);
                }
            }
        }

        // Create file group
        TestGroup _file = null;
        StringBuffer _key = new StringBuffer();
        if (!singleFile) {
            for (Node nCNode = xNode.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
                if (nCNode.getNodeName().equals("name")) {
                    String name = nCNode.getTextContent();
                    if (name != null && !name.isEmpty()) {
                        if (name != null && !name.isEmpty()) {
                            _key.append(name);
                        }
                        int hash = _key.toString().hashCode();
                        _file = files.get(hash);
                        if (_file == null) {
                            _file = new TestGroup(rootGroup, file.getRelativePath(), file.getFullPath(), TestValue.ClassType.ct_fileGrp);
                            files.put(hash, _file);
                            groups.put(hash, _file);
                            rootGroup.addGroup(_file);
                            detected.setFileDetected(true);
                        }
                        if (_duration != null && _duration > 0) {
                            String key = _key.toString() + "file_duration";
                            hash = key.hashCode();
                            DoubleValue file_duration = (DoubleValue) parameters.get(hash);
                            if (file_duration == null) {
                                file_duration = new DoubleValue(_file, null, "file_duration", TestValue.ClassType.ct_parameter);
                                parameters.put(hash, file_duration);
                                groups.put(hash, file_duration);
                                _file.addGroup(file_duration);
                            }
                            file_duration.setValue(build, _duration);
                        }
                        break;
                    }
                }
            }
        } else {
            _file = parent;
        }

        // Process related cases
        for (Node nCNode = xNode.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
            if (nCNode.getNodeName().equals("cases")) {
                ProcessXMLCases(_file, build, _key.toString(), nCNode, plugin, total_duration, keepLongStdio);
            }
        }
    }

    /**
     * Process group of cases and attach them to their parent file
     * @param parent File group
     * @param build Build number
     * @param xNode XML content
     * @param plugin Plugin parameter
     * @param total_duration parameter
     * @param keepLongStdio Keep long string messages
     */
    private void ProcessXMLCases(TestGroup parent, Integer build, String key, Node xNode, StringValue plugin, DoubleValue total_duration, BooleanValue keepLongStdio){
        for (Node nCNode = xNode.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
            if(nCNode.getNodeName().equals("case")){
                ProcessXMLCase(parent, build, key, nCNode, plugin, total_duration, keepLongStdio);
            }
        }
    }

    /**
     * Process case and attach them to their parent file
     * @param parent File group
     * @param build Build number
     * @param xNode XML content
     * @param plugin Plugin parameter
     * @param total_duration parameter
     * @param keepLongStdio Keep long string messages
     */
    private void ProcessXMLCase(TestGroup parent, Integer build, String key, Node xNode, StringValue plugin, DoubleValue total_duration, BooleanValue keepLongStdio){

        String      _group = null;
        Double      _duration = null;
        Integer     _failedSince = null;
        Map<String, String> _messages = new HashMap<String, String>();

        // Load parameters & file attribute
        for (Node nCNode = xNode.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
            String nodeText = nCNode.getNodeName();
            if (nodeText.equals("className")) {
                String text = nCNode.getTextContent();
                if (text != null && !text.isEmpty()) {
                    _group = text;
                }
            } else if (nodeText.equals("failedSince")) {
                String text = nCNode.getTextContent();
                if (text != null && !text.isEmpty()) {
                    _failedSince = Integer.parseInt(text);
                }
            } else if (nodeText.equals("skippedMessage")) {
                String text = nCNode.getTextContent();
                if (text != null && !text.isEmpty()) {
                    _messages.put("skippedMessage", text);
                }
            } else if (nodeText.equals("stdout")) {
                String text = nCNode.getTextContent();
                if (text != null && !text.isEmpty()) {
                    _messages.put("stdout", text);
                }
            } else if (nodeText.equals("stderr")) {
                String text = nCNode.getTextContent();
                if (text != null && !text.isEmpty()) {
                    _messages.put("stdout", text);
                }
            } else if (nodeText.equals("errorStackTrace")) {
                String text = nCNode.getTextContent();
                if (text != null && !text.isEmpty()) {
                    _messages.put("errorStackTrace", text);
                }
            } else if (nodeText.equals("errorDetails")) {
                String text = nCNode.getTextContent();
                if (text != null && !text.isEmpty()) {
                    _messages.put("errorDetails", text);
                }
            }
        }

        // Create result
        if (_failedSince != null) {
            String _name = "";
            StringBuffer _key = new StringBuffer(key);
            for (Node nCNode = xNode.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
                if (nCNode.getNodeName().equals("testName")) {
                    _name = nCNode.getTextContent();
                    if (_name != null && !_name.isEmpty()) {
                        if (_group != null && !_group.isEmpty()) {
                            _key.append(_group);
                        }
                        _key.append(_name);
                        int hash = _key.toString().hashCode();
                        StringValue result = (StringValue) results.get(hash);
                        if (result == null) {
                            result = new StringValue(parent, _group, _name);
                            results.put(hash, result);
                            groups.put(hash, result);
                            parent.addGroup(result);
                            detected.setGroupDetected(true);
                        }

                        if (_failedSince > 0) {
                            result.setValue(build, "Failed");
                            result.setFailedState(build, true);
                        } else {
                            result.setValue(build, "Passed");
                            result.setFailedState(build, false);
                        }
                        result.setMessages(build, _messages);
                        if (plugin != null) {
                            result.setParameter(build,plugin);
                        }
                        if (total_duration != null) {
                            result.setParameter(build, total_duration);
                        }
                        if (keepLongStdio != null) {
                            result.setParameter(build, keepLongStdio);
                        }
                        // Add failedSince parameter
                        hash = (_key.toString() + "FailedSince").hashCode();
                        IntegerValue failedSince = (IntegerValue)parameters.get(hash);
                        if (failedSince == null) {
                            failedSince = new IntegerValue(result, "FailedSince", null, TestValue.ClassType.ct_parameter);
                            parameters.put(hash, failedSince);
                            groups.put(hash, failedSince);
                            result.addGroup(failedSince);
                        }
                        failedSince.setValue(build, _failedSince);
                        if (plugin != null) {
                            failedSince.setParameter(build,plugin);
                        }
                        if (total_duration != null) {
                            failedSince.setParameter(build, total_duration);
                        }
                        if (keepLongStdio != null) {
                            failedSince.setParameter(build, keepLongStdio);
                        }
                        break;
                    }
                }
            }
            if (_failedSince == 0) {
                // Create additional duration result
                for (Node nCNode = xNode.getFirstChild(); nCNode != null; nCNode = nCNode.getNextSibling()) {
                    if (nCNode.getNodeName().equals("duration")) {
                        String text = nCNode.getTextContent();
                        if (text != null && !text.isEmpty()) {
                            _duration = Double.parseDouble(text);
                            if (_duration > 0) {
                                _key.append("duration");
                                int hash = _key.toString().hashCode();
                                DoubleValue result = (DoubleValue) results.get(hash);
                                if (result == null) {
                                    result = new DoubleValue(parent, _group, _name + ".duration");
                                    results.put(hash, result);
                                    groups.put(hash, result);
                                    parent.addGroup(result);
                                }
                                result.setValue(build, _duration);
                                if (plugin != null) {
                                    result.setParameter(build,plugin);
                                }
                                if (total_duration != null) {
                                    result.setParameter(build, total_duration);
                                }
                                if (keepLongStdio != null) {
                                    result.setParameter(build, keepLongStdio);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Determine if a file is a valid XML.
     * @param xmlFile Reference to XML file
     * @return Reference to XML document.
     */
    private Document getXML(File xmlFile) throws ParserConfigurationException, SAXException, IOException, InterruptedException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(xmlFile);
    }
}