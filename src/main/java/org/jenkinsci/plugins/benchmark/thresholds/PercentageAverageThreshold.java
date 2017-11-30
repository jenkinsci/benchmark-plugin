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
import hudson.Extension;
import hudson.model.Run;
import hudson.util.FormValidation;
import org.jenkinsci.plugins.benchmark.exceptions.ValidationException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.List;

/**
 * Percentage threshold compared to average
 *
 * @author Daniel Mercier
 * @since 5/16/2017
 */
public class PercentageAverageThreshold extends Threshold {

    // Variables
    private final Double percentage;
    private Double average;

    // Constructor
    @DataBoundConstructor
    public PercentageAverageThreshold(String testGroup, String testName, Double percentage){
        super(testGroup, testName, ThresholdTypes.tt_percentageAverage);
        this.percentage = percentage;
        this.average = null;
    }

    public PercentageAverageThreshold(Double percentage) throws ValidationException{
        super(ThresholdTypes.tt_percentageAverage);
        if (percentage == null){
            throw new ValidationException(Messages.PercentageAverageThreshold_MissingPercentageValue());
        }
        this.percentage = percentage;
        this.average = null;
    }

    // Functions
    public boolean evaluate(List<? extends Run<?, ?>> builds) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, AbortException, ParseException {
        return true;
    }

    @Override
    public boolean isValid(int value) throws NullPointerException, ValidationException {
        if ( average == null )
            return true;
        double calculatedPercentage = Math.abs((value - average) / average) * 100;
        if (percentage != null && calculatedPercentage > percentage) {
            throw new ValidationException(Messages.PercentageAverageThreshold_OutOfPercentageFromAverage(Integer.toString(value), Double.toString(percentage), Double.toString(average)));
        }
        return true;
    }

    @Override
    public boolean isValid(double value) throws NullPointerException, ValidationException{
        if ( average == null )
            return true;
        double calculatedPercentage = Math.abs((value - average) / average) * 100;
        if (percentage != null && calculatedPercentage > percentage) {
            throw new ValidationException(Messages.PercentageAverageThreshold_OutOfPercentageFromAverage(Double.toString(value), Double.toString(percentage), Double.toString(average)));
        }
        return true;
    }

    // Setter
    public void setAverageValue(Double average){ this.average = average; }

    // Getter
    public Double getPercentage() { return percentage; }
    public Double getAverageValue() { return average; }

    // Descriptor (active interactor)
    @Extension
    public static class DescriptorImpl extends ThresholdDescriptor {

        @Override
        public String getDisplayName() {return Messages.PercentageAverageThreshold_PercentageFromAverage();}

        public FormValidation doCheckPercentage(@QueryParameter Double percentage) {
            if (percentage == null) {
                return FormValidation.error(Messages.PercentageAverageThreshold_PercentageCannotBeEmpty());
            }
            if (100 < percentage || percentage < 0){
                return FormValidation.error(Messages.PercentageAverageThreshold_PercentageBetween0And100());
            }
            return FormValidation.ok();
        }
    }
}
