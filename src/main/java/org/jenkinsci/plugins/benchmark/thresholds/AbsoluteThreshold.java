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
 * Absolute Threshold with [min, max] values
 *
 * @author Daniel Mercier
 * @since 5/16/2017
 */
public class AbsoluteThreshold extends Threshold {

    // Variables
    private final Double minimum;
    private final Double maximum;

    // Constructor
    @DataBoundConstructor
    public AbsoluteThreshold(String testGroup, String testName, Double minimum, Double maximum) {
        super(testGroup, testName, ThresholdTypes.tt_absolute);
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public AbsoluteThreshold(Double minimum, Double maximum) throws ValidationException {
        super(ThresholdTypes.tt_absolute);
        if (minimum == null && maximum == null){
            throw new ValidationException(Messages.AbsoluteThreshold_MissingMinAndMax());
        }
        this.minimum = minimum;
        this.maximum = maximum;
    }

    // Functions
    public boolean evaluate(List<? extends Run<?, ?>> builds) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, AbortException, ParseException {
        return true;
    }

    @Override
    public boolean isValid(int value) throws NullPointerException, ValidationException {
        if (minimum != null && value < minimum) {
            throw new ValidationException(Messages.AbsoluteThreshold_IsBelowMinimum(Integer.toString(value), Double.toString(minimum)));
        }
        if (maximum != null && maximum < value) {
            throw new ValidationException(Messages.AbsoluteThreshold_IsAboveMaximum(Integer.toString(value), Double.toString(maximum)));
        }
        return true;
    }

    @Override
    public boolean isValid(double value) throws NullPointerException, ValidationException {
        if (minimum != null && value < minimum) {
            throw new ValidationException(Messages.AbsoluteThreshold_IsBelowMinimum(Double.toString(value),Double.toString(minimum)));
        }
        if (maximum != null &&maximum < value) {
            throw new ValidationException(Messages.AbsoluteThreshold_IsAboveMaximum(Double.toString(value), Double.toString(maximum)));
        }
        return true;
    }

    // Getters

    public Double getMinimum() { return minimum; }
    public Double getMaximum() { return maximum; }

    // Descriptor (active interactor)
    @Extension
    public static class DescriptorImpl extends ThresholdDescriptor {

        @Override
        public String getDisplayName() { return Messages.AbsoluteThreshold_AbsoluteValues(); }

        public FormValidation doCheckMaximum(@QueryParameter Double minimum, @QueryParameter Double maximum) {
            if (minimum != null && maximum != null && minimum > maximum) {
                return FormValidation.error(Messages.AbsoluteThreshold_MaxIsBelowMin());
            }
            return FormValidation.ok();
        }
    }

}
