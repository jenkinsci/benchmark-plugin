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
package org.jenkinsci.plugins.benchmark.thresholds;

import hudson.AbortException;
import hudson.ExtensionList;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Run;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.benchmark.exceptions.ValidationException;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.List;

/**
 * Threshold base class
 *
 * CAREFULL, this is connected to JELLY
 * @author Daniel Mercier
 * @since 5/16/2017.
 */
public abstract class Threshold extends AbstractDescribableImpl<Threshold> {

    // Enumeration
    public enum ThresholdTypes {    // Threshold method options
        tt_unknown,
        tt_absolute,
        tt_percentage,
        tt_percentageAverage,
        tt_delta,
        tt_deltaAverage
    }

    // Variables
    private ThresholdTypes type;    // Threshold type(see thresholdTypes for options)
    private String          testGroup;
    private String          testName;

    // Constructor
    protected Threshold(String testGroup, String testName, ThresholdTypes type) {
        this.type = type;
        this.testGroup = testGroup;
        this.testName = testName;
    }

    protected Threshold(ThresholdTypes type) {
        this.type = type;
        this.testGroup = "";
        this.testName = "";
    }

    // Functions
    public static ExtensionList<Threshold> all() {
        return Jenkins.getInstance().getExtensionList(Threshold.class);
    }

    public ThresholdDescriptor getDescriptor() {
        return (ThresholdDescriptor) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    public boolean isValid(int value) throws NullPointerException, ValidationException { return true; }
    public boolean isValid(double value) throws NullPointerException, ValidationException { return true; }


    // Abstract functions
    /**
     * Evaluates whether the threshold is activated or not
     *
     * @param builds all builds that are saved in Jenkins
     * @return Successful evaluation
     * @throws IllegalArgumentException if illegal argument
     * @throws IllegalAccessException If illegal access
     * @throws InvocationTargetException If invocation incorrect
     * @throws AbortException If action aborded
     * @throws ParseException If parse failed
     */
    public abstract boolean evaluate(List<? extends Run<?, ?>> builds) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, AbortException, ParseException;

    // Setter

    public void setAverageValue(Double average){}
    public void setPreviousValue(Double average){}

    // Getters

    public String getName(){
        switch(type){
            case tt_absolute:
                return Messages.Threshold_AbsoluteThreshold();
            case tt_percentage:
                return Messages.Threshold_PercentageFromLastThreshold();
            case tt_percentageAverage:
                return Messages.Threshold_PercentageFromAverageThreshold();
            case tt_delta:
                return Messages.Threshold_DeltaFromLastThreshold();
            case tt_deltaAverage:
                return Messages.Threshold_DeltaFromAverageThreshold();
            default:
                return Messages.Threshold_UnknownThreshold();
        }
    }
    public ThresholdTypes getType() { return type; }
    public String getTestGroup() { return testGroup; }
    public String getTestName() { return testName; }

}
