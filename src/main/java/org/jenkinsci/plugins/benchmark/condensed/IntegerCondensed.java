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
package org.jenkinsci.plugins.benchmark.condensed;

import org.jenkinsci.plugins.benchmark.results.IntegerValue;
import org.jenkinsci.plugins.benchmark.results.Messages;
import org.jenkinsci.plugins.benchmark.results.TestGroup;
import org.jenkinsci.plugins.benchmark.utilities.ContentDetected;

import java.util.TreeSet;

/**
 * Holds the information for integer condensed result
 *
 * @author Daniel Mercier
 * @since 5/10/2017
 */
public class IntegerCondensed extends IntegerValue {

    // Variables

    private final Integer       previous;
    private final Integer       minimum;
    private final Integer       maximum;
    private final Double        average;
    private final Double        std_deviation;

    private final int           passed;
    private final int           failed;

    // Constructor

    public IntegerCondensed(TestGroup parent, String group, String name, String description, String unit, Integer previous, Integer minimum, Integer maximum, Double average, Double std_deviation, int passed, int failed){
        super(parent, group, name, description, unit);
        this.previous = previous;
        this.minimum = minimum;
        this.maximum = maximum;
        this.average = average;
        this.std_deviation = std_deviation;
        this.passed = passed;
        this.failed = failed;
    }

    // Functions

    /**
     * Assemble the HTML content to display the condensed table [TABLE PAGE]
     * @param key Result key
     * @param detected Key characteristics of results
     * @param decimalSeparator Decimal separator
     * @return HTML content for condensed table
     */
    @Override
    public String getHTMLCondensed(Integer key, ContentDetected detected, char decimalSeparator) {
        StringBuffer content = new StringBuffer();
        content.append("<tr><td>");
        if (detected.isFileDetected()){
            if (this.parent == null) {
                content.append("</td><td>");
            } else {
                content.append(this.parent.getName());
                content.append("</td><td>");
            }
        }
        if (detected.isGroupDetected()) {
            if (this.group == null) {
                content.append("</td><td>");
            } else {
                content.append(this.group);
                content.append("</td><td>");
            }
        }
        content.append(this.getName());
        content.append("</td><td>");
        if (detected.isUnitsDetected()) {
            if (this.unit == null) {
                content.append("-</td><td>");
            } else {
                content.append(this.unit);
                content.append("</td><td>");
            }
        }
        content.append(key.toString());
        content.append("</td><td>");
        if (average != null) {
            content.append(this.minimum.toString().replace('.', decimalSeparator));
            content.append("</td><td>");
            content.append(this.maximum.toString().replace('.',decimalSeparator));
            content.append("</td><td>");
            content.append(String.format("%f", this.average));
            content.append("</td><td>");
            content.append(String.format("%f", this.std_deviation));
            content.append("</td><td>");
        }
        content.append(Integer.toString(this.passed));
        content.append("</td><td>");
        content.append(Integer.toString(this.failed));
        content.append("</td></tr>");
        return content.toString();
    }

    /**
     * Assemble the HTML content to display the condensed table [DETAIL PAGE]
     * @param detected Key characteristics of results
     * @param decimalSeparator Decimal separator
     * @return HTML content
     */
    @Override
    public String getHTMLCondensedDetail(ContentDetected detected, char decimalSeparator) {
        StringBuffer content = new StringBuffer();
        if (average != null) {
            content.append("<tr><td>");
            content.append(Messages.Minimum());
            content.append("</td><td>");
            content.append(Integer.toString(this.minimum));
            content.append("</td></tr><tr><td>");
            content.append(Messages.Maximum());
            content.append("</td><td>");
            content.append(Integer.toString(this.maximum));
            content.append("</td></tr><tr><td>");
            content.append(Messages.MeanAverage());
            content.append("</td><td>");
            content.append(String.format("%6g", this.average));
            content.append("</td></tr><tr><td>");
            content.append(Messages.StdDeviation());
            content.append("</td><td>");
            content.append(String.format("%6g", this.std_deviation));
            content.append("</td></tr>");
        }
        content.append("<tr><td>");
        content.append(Messages.NumberOFPassedTests());
        content.append("</td><td>");
        content.append(Integer.toString(this.passed));
        content.append("</td></tr><tr><td>");
        content.append(Messages.NumberOfFailedTests());
        content.append("</td><td>");
        content.append(Integer.toString(this.failed));
        content.append("</td></tr>");
        return content.toString();
    }

    /**
     * Assemble the CSV content to display the condensed table [CSV EXPORT]
     * @param detected Key characteristics of results
     * @return CSV content
     */
    @Override
    public String getCSVCondensed(ContentDetected detected) {
        StringBuffer content = new StringBuffer();
        if (detected.isFileDetected()){
            if (this.parent == null) {
                content.append(',');
            } else {
                content.append(this.parent.getName());
                content.append(',');
            }
        }
        if (detected.isGroupDetected()) {
            if (this.group == null) {
                content.append(',');
            } else {
                content.append(this.group);
                content.append(',');
            }
        }
        content.append(this.getName());
        if (detected.isUnitsDetected()) {
            if (this.unit == null) {
                content.append(",-");
            } else {
                content.append(',');
                content.append(this.unit);
            }
        }
        if (average != null) {
            content.append(',');
            content.append(this.minimum.toString());
            content.append(',');
            content.append(this.maximum.toString());
            content.append(',');
            content.append(this.average.toString());
            content.append(',');
            content.append(this.std_deviation.toString());
        }
        content.append(',');
        content.append( Integer.toString(this.passed));
        content.append(',');
        content.append(Integer.toString(this.failed));
        return content.toString();
    }

    // Getters

    public Integer getPrevious() {return previous;}
    public Double getAverage() { return average; }
    public Double getStdDeviation() { return std_deviation; }
    @Override
    public Double getMinimum() { return minimum.doubleValue(); }
    @Override
    public Double getMaximum() { return maximum.doubleValue(); }
    public int getPassed() { return passed; }
    public int getFailed() { return failed; }
}
