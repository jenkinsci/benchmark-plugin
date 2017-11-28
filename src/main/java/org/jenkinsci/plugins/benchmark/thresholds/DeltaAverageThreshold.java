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
import java.lang.Math;

/**
 * Delta value threshold compared to last build
 *
 * @author Daniel Mercier
 * @since 5/16/2017
 */
public class DeltaAverageThreshold extends Threshold{

    // Variables
    private final Double delta;
    private Double average;

    // Constructor
    @DataBoundConstructor
    public DeltaAverageThreshold(String testGroup, String testName, Double delta){
        super(testGroup,testName, ThresholdTypes.tt_deltaAverage);
        this.delta = delta;
        this.average = null;
    }


    public DeltaAverageThreshold(Double delta) throws ValidationException{
        super(ThresholdTypes.tt_deltaAverage);
        if (delta == null){
            throw new ValidationException(Messages.DeltaAverageThreshold_MissingDelta());
        }
        this.delta = delta;
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
        double calculatedDelta = Math.sqrt((value - average)*(value - average));
        if ( delta != null && calculatedDelta > delta) {
            throw new ValidationException(Messages.DeltaAverageThreshold_OutOfDeltaFromAverage(Integer.toString(value), Double.toString(delta), Double.toString(average)));
        }
        return true;
    }

    @Override
    public boolean isValid(double value) throws NullPointerException, ValidationException {
        if ( average == null )
            return true;
        double calculatedDelta = Math.sqrt((value - average)*(value - average));
        if ( delta != null && calculatedDelta > delta) {
            throw new ValidationException(Messages.DeltaAverageThreshold_OutOfDeltaFromAverage(Double.toString(value), Double.toString(delta), Double.toString(average)));
        }
        return true;
    }

    // Setter
    public void setAverageValue(Double average){ this.average = average; }

    // Getter
    public Double getDelta() { return delta; }
    public Double getAverageValue() { return average; }

    // Descriptor (active interactor)
    @Extension
    public static class DescriptorImpl extends ThresholdDescriptor {

        @Override
        public String getDisplayName() { return Messages.DeltaAverageThreshold_DeltaFromAverage(); }

        public FormValidation doCheckDelta(@QueryParameter Double delta) {
            if (delta == null) {
                return FormValidation.error(Messages.DeltaAverageThreshold_DeltaCannotBeEmpty());
            }
            if (delta < 0) {
                return FormValidation.error(Messages.DeltaAverageThreshold_DeltaAboveOrEqualTo0());
            }
            return FormValidation.ok();
        }
    }
}
