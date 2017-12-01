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
package org.jenkinsci.plugins.benchmark.parsers;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.benchmark.exceptions.ValidationException;
import org.jenkinsci.plugins.benchmark.parsers.JsonToPlugin.MapJsonToPlugin;
import org.jenkinsci.plugins.benchmark.parsers.XmlToPlugin.MapXmlToPlugin;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Format selector based on selected information
 *
 * @author Daniel Mercier
 * @since 6/20/2017.
 */
public class FormatSelector {

    // Variable

    MapperBase mapper;

    // Constructor

    public FormatSelector(Run<?, ?> run, FilePath filePath, String searchFilePattern, String schema, boolean truncateStrings, TaskListener listener) throws InterruptedException, ValidationException, IOException {

        if (schema == null || schema.length() == 0) {
            throw new IOException(Messages.FormatSelector_SchemaIsEmpty());
        }
        if (searchFilePattern == null || searchFilePattern.length() == 0) {
            throw new IOException(Messages.FormatSelector_FileSearchPatternIsEmpty());
        }

        JsonElement jSchema = null;
        Document xSchema = null;
        try {
            jSchema = getJSON(schema);
        } catch (Exception e) {
            try {
                xSchema = getXML(schema);
            } catch (Exception e2) {
                throw new IOException(Messages.FormatSelector_NoCompatibleSchemaFromatRecognised());
            }
        }

        // Execute the mapping
        if (!filePath.isDirectory()) {
            listener.getLogger().println(Messages.FormatSelector_WorkspaceNotDetected());
            throw new IOException(Messages.FormatSelector_WorkspaceNotDetected());
        }

        int buildNumber = run.getNumber();

        // JSON
        if (jSchema != null) {
            Map<String, FilePath> files;
            try {
                listener.getLogger().println(Messages.FormatSelector_FilePattern(searchFilePattern));
                files = IdentifyFiles(filePath, searchFilePattern, "json");
            } catch (Exception e) {
                throw new ValidationException(Messages.FormatSelector_FilePatternCannotBeParsed(searchFilePattern));
            }
            if (files.size() == 0) {
                throw new IOException(Messages.FormatSelector_NoJsonDetectedInFilePattern(searchFilePattern));
            }
            MapJsonToPlugin plugin = null;
            if (files.size() == 1) {
                for (Map.Entry<String, FilePath> file : files.entrySet()) {
                    plugin = new MapJsonToPlugin(buildNumber, file.getValue(), jSchema, truncateStrings);
                    break;
                }
            } else {
                plugin = new MapJsonToPlugin(buildNumber, files, jSchema, truncateStrings, listener);
            }
            mapper = (MapperBase) plugin;
            return;
        }

        // XML
        if (xSchema != null) {
            Map<String, FilePath> files;
            try {
                listener.getLogger().println(Messages.FormatSelector_FilePattern(searchFilePattern));
                files = IdentifyFiles(filePath, searchFilePattern, "xml");
            } catch (Exception e) {
                throw new ValidationException(Messages.FormatSelector_FilePatternCannotBeParsed(searchFilePattern));
            }
            if (files.size() == 0) {
                throw new IOException(Messages.FormatSelector_NoXmlDetectedInFilePattern(searchFilePattern));
            }
            MapXmlToPlugin plugin = null;
            if (files.size() == 1) {
                for (Map.Entry<String, FilePath> file : files.entrySet()) {
                    plugin = new MapXmlToPlugin(buildNumber, file.getValue(), xSchema, truncateStrings);
                    break;
                }
            } else {
                plugin = new MapXmlToPlugin(buildNumber, files, xSchema, truncateStrings, listener);
            }
            mapper = (MapperBase) plugin;
        }
    }

    // Functions


    public static void checkFormat(String schema) throws IOException {
        try {
            getJSON(schema);
        } catch (Exception e) {
            try {
                getXML(schema);
            } catch (Exception e2) {
                throw new IOException(Messages.FormatSelector_NoCompatibleSchemaFromatRecognised());
            }
        }
    }


    /**
     * Identify the existing files based on search file pattern with wildcard
     * @param fileBase Build workspace address
     * @param searchFilePattern File search pattern to identify result files
     * @param extension File extension
     * @throws IOException I/O exception
     * @throws InterruptedException Interrupted exception
     * @return Map with shorten filename and file address
     */
    public static Map<String, FilePath> IdentifyFiles (FilePath fileBase, String searchFilePattern, String extension) throws IOException, InterruptedException {

        String[] individuals = searchFilePattern.split("\\;|\\,");

        // Identify lists and add their content
        List<String[]> listOfLists = new ArrayList<String[]>();
        List<String[]> listChunks = new ArrayList<String[]>();
        for (String individual : individuals) {
            String[] chunks = individual.split("\\\\|\\/");
            if (isFileWithExtension(individual, "list")) {
                listOfLists.add(chunks);
            } else {
                listChunks.add(chunks);
            }
        }

        // Locate lists, extract content of lists and add it to list of files
        Map<String, FilePath> listFiles = processChunks(fileBase, "", "", 0, listOfLists, "list");
        for (Map.Entry<String, FilePath> entry:listFiles.entrySet()) {
            String content;
            try {
                InputStream inputStream = entry.getValue().read();
                content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            } catch (Exception e) {
                continue;
            }
            String[] addIndividuals = content.split("\\;|\\,|\\r?\\n");
            for (String individual : addIndividuals) {
                if (!individual.isEmpty()) {
                    String[] chunks = individual.split("\\\\|\\/");
                    listChunks.add(chunks);
                }
            }
        }
        return processChunks(fileBase, "", "", 0, listChunks, extension);
    }

    /**
     *  Recursive  identification of input files considering wildcard
     *
     * @param shortName Condensed path name to detected file location
     * @param pathName Full path name to detected file location
     * @param index Location identified content inside the active string
     * @param listChunks List of chunks to check to identify active files
     * @param extension File extension that determine format for processing
     * @return Map with shorten filename and file address
     */
    private static Map<String, FilePath> processChunks(FilePath fileBase, String shortName, String pathName, int index, List<String[]> listChunks, String extension) throws IOException, InterruptedException {

        int numOriginal = listChunks.size();
        Map<String, FilePath> result = new HashMap<String, FilePath>();
        for (String[] individual : listChunks) {
            if (individual.length < index + 1) {
                continue;
            }
            List<String[]> list = new ArrayList<String[]>();
            String marker = individual[index];
            for (String[] individual_2 : listChunks) {
                if (individual_2.length > index) {
                    if (marker.equals(individual_2[index])) {
                        list.add(individual_2);
                    }
                }
            }
            if (list.size() == 0)
                break;

            if (marker.contains("*")) {
                String[] pieces = marker.split("\\*");
                FilePath path = new FilePath(fileBase, pathName);
                List<FilePath> files = path.list();
                for (FilePath file:files){
                    boolean detectedField = false;
                    String name = file.getName();
                    String nextShortName = shortName;
                    String nextPathName = pathName + "\\" + name;

                    // Check compatible with
                    int baseIndex = 0, newIndex;
                    for (String piece:pieces){
                        newIndex = name.indexOf(piece, baseIndex);
                        if (newIndex == -1) {
                            nextShortName = null;
                            break;
                        }
                        if (baseIndex != newIndex) {
                            detectedField = true;
                        }
                        baseIndex = newIndex + piece.length();

                    }
                    if (nextShortName != null && baseIndex < name.length()) {
                        detectedField = true;
                    }
                    if (detectedField){
                        if (nextShortName.length() == 0) {
                            nextShortName = name;
                        } else {
                            nextShortName += "\\" + name;
                        }
                    }

                    if (nextShortName != null) {
                        for (String[] individual_2:list) {
                            String[] newIndividual = individual_2.clone();
                            newIndividual[index] = file.getName();
                        }

                        FilePath nextFile = new FilePath(fileBase, nextPathName);
                        if (nextFile.isDirectory()) {
                            result.putAll(processChunks(fileBase, nextShortName, nextPathName, index + 1, list, extension));
                        } else if (file.exists() && isFileWithExtension(nextFile, extension)) {
                            result.put(nextShortName, nextFile);
                        }
                    }
                }
            } else {
                String nextShortName = shortName;
                if (numOriginal != list.size()) {
                    if (nextShortName.length() == 0 ){
                        nextShortName += marker;
                    } else {
                        nextShortName += "\\" + marker;
                    }
                }
                String nextPathName = "";
                if (pathName.length() == 0) {
                    nextPathName = marker;
                } else {
                    nextPathName = pathName + "\\" + marker;
                }

                FilePath file = new FilePath(fileBase, nextPathName);
                if (file.isDirectory()) {
                    result.putAll(processChunks(fileBase, nextShortName, nextPathName, index + 1, list, extension));
                } else if (file.exists() && isFileWithExtension(file, extension)) {
                    result.put(nextShortName, file);
                }
            }
        }
        return result;
    }

    /**
     * Confirm whether the filename has the right extension
     *
     * @param file File name
     * @param extension Extension to search for
     * @return Whether file has extension
     */
    private static boolean isFileWithExtension(String file, String extension) {

        try {
            return extension.equalsIgnoreCase(file.substring(file.lastIndexOf(".") + 1));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Confirm whether the filename has the right extension
     *
     * @param file File name
     * @param extension Extension to search for
     * @return Whether file has extension
     */
    private static boolean isFileWithExtension(FilePath file, String extension) {

        String name = file.getName();
        try {
            return extension.equalsIgnoreCase(name.substring(name.lastIndexOf(".") + 1));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Determine if a string is a valid JSON.
     * @param sContent String with Json content
     * @return Json Element in GSON format
     */
    private static JsonElement getJSON(String sContent) throws JsonIOException, JsonSyntaxException{
        JsonParser parser = new JsonParser();
        JsonElement content = parser.parse(sContent);
        return content;
    }

    /**
     * Determine if a string is a valid XML.
     * @param xmlContent XML content file
     * @return Reference to XML document.
     */
    private static Document getXML(String xmlContent) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xmlContent)));
    }

    //  Getter

    public MapperBase getMapper() {
        return mapper;
    }
}