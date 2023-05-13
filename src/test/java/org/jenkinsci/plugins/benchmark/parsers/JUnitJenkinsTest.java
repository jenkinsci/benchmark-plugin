/**
 * MIT LICENSE.txt
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

import org.jenkinsci.plugins.benchmark.exceptions.ValidationException;
import org.jenkinsci.plugins.benchmark.utilities.FilePathGroup;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.jenkinsci.plugins.benchmark.parsers.JUnitJenkins.RecurseNames;
import static org.junit.Assert.assertTrue;

/**
 * Test of the Jenkins jUnit mapper
 *
 * @author Daniel Mercier
 * @since 6/20/2017.
 */
public class JUnitJenkinsTest {

    @Test
    public void JUnitJenkins_mapper() throws InterruptedException, ValidationException, ParserConfigurationException, SAXException, IOException {
        System.out.println("Starting XML mapping for jUnit Jenkins format.");

        boolean truncateStrings = false;
        ClassLoader testClassLoader = getClass().getClassLoader();
        File xmlCFile = new File(testClassLoader.getResource("xml/jUnitJenkinsResult.xml").getFile());

        JUnitJenkins mapper = new JUnitJenkins(0, truncateStrings);
        mapper.importFromFile(0, xmlCFile);

        assertTrue(mapper.getNumberOfResults() == 8);
        assertTrue(mapper.getNumberOfParameters() == 6);

        System.out.println("Completed XML mapping for jUnit Jenkins format.");
    }

    @Test
    public void JUnitJenkins_FileProcessor() throws InterruptedException, ValidationException, ParserConfigurationException, SAXException, IOException {
        System.out.println("Starting Filename processor.");

        List<FilePathGroup> names = new ArrayList<FilePathGroup>();
        names.add(new FilePathGroup("c:\\folder_1\\folder_2\\folder_3\\file_1.xml"));
        names.add(new FilePathGroup("c:\\folder_1\\folder_2\\folder_3\\file_2.xml"));
        names.add(new FilePathGroup("c:\\folder_4\\workspace\\folder_3\\file_2.xml"));
        names.add(new FilePathGroup("c:\\folder_4\\workspace\\folder_3\\file_4.xml"));
        names.add(new FilePathGroup("c:\\folder_4\\workspace\\folder_6\\file_3.xml"));
        try {
            RecurseNames(false, 0, "", names);
            assert(names.get(4).getNumberOfSegments() == 5);
            assert(names.get(4).getPathSegment(4).equals("file_3.xml"));
        } catch (Exception e) {
            throw( new ValidationException("Error detected during recurse processing of filenames."));
        }

        System.out.println("Completed Filename processor.");
    }
}
