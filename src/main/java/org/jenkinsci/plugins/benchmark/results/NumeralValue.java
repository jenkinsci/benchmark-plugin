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
package org.jenkinsci.plugins.benchmark.results;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jenkinsci.plugins.benchmark.utilities.ContentDetected;
import org.jenkinsci.plugins.benchmark.utilities.TextToHTML;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Holds the information for numeral test result
 *
 * @author Daniel Mercier
 * @since 5/10/2017
 */
public class NumeralValue extends TestValue {

    // Variables


    // Constructor

    NumeralValue(TestGroup parent, String name, String description, String unit, ValueType type) {
        super(parent, null, name, description, unit, type, ClassType.ct_result);
    }

    NumeralValue(TestGroup parent, String group, String name, String description, String unit, ValueType type) {
        super(parent, group, name, description, unit, type, ClassType.ct_result);
    }

    NumeralValue(TestGroup parent, String name, String description, String unit, ValueType type, ClassType ctype) {
        super(parent, null, name, description, unit, type, ctype);
    }

    NumeralValue(TestGroup parent, String group, String name, String description, String unit, ValueType type, ClassType ctype) {
        super(parent, group, name, description, unit, type, ctype);
    }

    // Getters

    /**
     * Get minimum
     * @return minimum if available
     */
    public Double getMinimum() { return 0.0;}

    /**
     * Get minimum
     * @return minimum if available
     */
    public Double getMaximum() { return 0.0;}

}
