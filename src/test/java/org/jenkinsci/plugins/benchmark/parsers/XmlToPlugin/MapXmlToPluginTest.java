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

import org.jenkinsci.plugins.benchmark.exceptions.ValidationException;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Test of the XML mapper
 *
 * @author Daniel Mercier
 * @since 6/20/2017.
 */
public class MapXmlToPluginTest {
    @Test
    public void xml_simplestResult() throws ValidationException, ParserConfigurationException, SAXException, IOException {
        System.out.println("Starting XML mapping for '1-simpleResult {boolean result, key failure}'.");

        DocumentBuilder builder = this.createDocumentBuilder();

        // Load schema
        ClassLoader classLoader = MapXmlToPlugin.class.getClassLoader();
        File xmlSFile = new File(classLoader.getResource("schemas/simplest.xml").getFile());
        Document xmlSchema = builder.parse(xmlSFile);
        xmlSchema.getDocumentElement().normalize();

        // Load content
        ClassLoader testClassLoader = getClass().getClassLoader();
        File xmlCFile = new File(testClassLoader.getResource("xml/simplest.xml").getFile());

        // Launch mapper
        MapXmlToPlugin mapper = new MapXmlToPlugin(0, xmlCFile, xmlSchema, false);
        assertTrue(mapper.getNumberOfResults() == 1);

        System.out.println("Mapping XML completed for '1-simpleResult {boolean result, key failure}'.");
    }

    @Test
    public void xml_defaultSchema() throws ValidationException, ParserConfigurationException, SAXException, IOException {
        System.out.println("Starting XML mapping for '2-defaultSchema {all construct types}'.");

        DocumentBuilder builder = this.createDocumentBuilder();

        // Load schema
        ClassLoader classLoader = MapXmlToPlugin.class.getClassLoader();
        File xmlSFile = new File(classLoader.getResource("schemas/default.xml").getFile());
        Document xmlSchema = builder.parse(xmlSFile);
        xmlSchema.getDocumentElement().normalize();

        // Load content
        ClassLoader testClassLoader = getClass().getClassLoader();
        File xmlCFile = new File(testClassLoader.getResource("xml/default.xml").getFile());

        // Launch mapper
        MapXmlToPlugin mapper = new MapXmlToPlugin(0, xmlCFile, xmlSchema, false);
        assertTrue(mapper.getNumberOfResults() == 2);

        System.out.println("Mapping XML completed for '2-defaultSchema {all construct types}'.");
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

        factory.setNamespaceAware(true);

        return factory.newDocumentBuilder();
    }
}
