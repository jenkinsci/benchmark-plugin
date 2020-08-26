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
package org.jenkinsci.plugins.benchmark.parsers.JsonToPlugin;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import hudson.FilePath;
import org.jenkinsci.plugins.benchmark.exceptions.ValidationException;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertTrue;

/**
 * Test of the JSON mapper
 *
 * @author Daniel Mercier
 * @since 6/20/2017.
 */
public class MapJsonToPluginTest {
    @Test
    public void json_simplestResult() throws ValidationException, ParserConfigurationException, SAXException, IOException {
        System.out.println("Starting JSON mapping for '1-simpleResult {boolean result, key failure}'." );

        // Load schema
        ClassLoader classLoader = MapJsonToPlugin.class.getClassLoader();
        File jsonSFile = new File(classLoader.getResource("schemas/simplest.json").getFile());
        JsonParser parser = new JsonParser();
        InputStreamReader reader = new InputStreamReader(new FileInputStream(jsonSFile), StandardCharsets.UTF_8);
        try {
            JsonElement jsonSchema = parser.parse(reader);

            // Load content
            ClassLoader testClassLoader = getClass().getClassLoader();
            File jsonCFile = new File(testClassLoader.getResource("json/simplest.json").getFile());

            // Launch mapper
            MapJsonToPlugin mapper = new MapJsonToPlugin(0, jsonCFile, jsonSchema, false);
            assertTrue(mapper.getNumberOfResults() == 1);
        }finally{
            reader.close();
        }
        System.out.println("Mapping JSON completed for '1-simpleResult {boolean result, key failure}'." );
    }

    @Test
    public void json_defaultSchema() throws ValidationException, ParserConfigurationException, SAXException, IOException {
        System.out.println("Starting JSON mapping for '2-defaultSchema {all construct types}'." );

        // Load schema
        ClassLoader classLoader = MapJsonToPlugin.class.getClassLoader();
        File jsonSFile = new File(classLoader.getResource("schemas/default.json").getFile());
        JsonParser parser = new JsonParser();
        InputStreamReader reader = new InputStreamReader(new FileInputStream(jsonSFile), StandardCharsets.UTF_8);
        try {
            JsonElement jsonSchema = parser.parse(reader);

            // Load content
            ClassLoader testClassLoader = getClass().getClassLoader();
            File jsonCFile = new File(testClassLoader.getResource("json/default.json").getFile());

            // Launch mapper
            MapJsonToPlugin mapper = new MapJsonToPlugin(0, jsonCFile, jsonSchema, false);
            assertTrue(mapper.getNumberOfResults() == 4);
        }finally{
            reader.close();
        }
        System.out.println("Mapping JSON completed for '2-defaultSchema {all construct types}'." );
    }

    @Test
    public void json_mappedSchema() throws ValidationException, ParserConfigurationException, SAXException, IOException {
        System.out.println("Starting JSON mapping for '3-mappedSchema {additional properties}'." );

        // Load schema
        ClassLoader testClassLoader = getClass().getClassLoader();
        File jsonSFile = new File(testClassLoader.getResource("schemas/mapped.json").getFile());
        JsonParser parser = new JsonParser();
        InputStreamReader reader = new InputStreamReader(new FileInputStream(jsonSFile), StandardCharsets.UTF_8);
        try {
            JsonElement jsonSchema = parser.parse(reader);

            // Load content
            File jsonCFile = new File(testClassLoader.getResource("json/mapped.json").getFile());

            // Launch mapper
            MapJsonToPlugin mapper = new MapJsonToPlugin(0, jsonCFile, jsonSchema, false);
            assertTrue(mapper.getNumberOfResults() == 1);
        }finally{
            reader.close();
        }
        System.out.println("Mapping JSON completed for '3-mappedSchema {additional properties}'." );
    }

    @Test
    public void json_condensedSchema() throws ValidationException, ParserConfigurationException, SAXException, IOException {
        System.out.println("Starting JSON mapping for '4-condensedSchema {additional properties, full results}'." );

        // Load schema
        ClassLoader testClassLoader = getClass().getClassLoader();
        File jsonSFile = new File(testClassLoader.getResource("schemas/condensed.json").getFile());
        JsonParser parser = new JsonParser();
        InputStreamReader reader = new InputStreamReader(new FileInputStream(jsonSFile), StandardCharsets.UTF_8);
        try {
            JsonElement jsonSchema = parser.parse(reader);

            // Load content
            File jsonCFile = new File(testClassLoader.getResource("json/condensed.json").getFile());

            // Launch mapper
            MapJsonToPlugin mapper = new MapJsonToPlugin(0, jsonCFile, jsonSchema, false);
            assertTrue(mapper.getNumberOfResults() == 2);
        }finally{
            reader.close();
        }
        System.out.println("Mapping JSON completed for '4-condensedSchema {additional properties, full results}'." );
    }
}
