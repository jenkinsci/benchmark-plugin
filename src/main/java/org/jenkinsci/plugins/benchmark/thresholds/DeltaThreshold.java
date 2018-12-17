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
 * Delta value threshold compared to last build
 *
 * @author Daniel Mercier
 * @since 5/16/2017
 */
public class DeltaThreshold extends Threshold{

    // Variables
    private final Double delta;
    private final boolean ignoreNegativeDeltas;
    private Double previous;

    // Constructor
    @DataBoundConstructor
    public DeltaThreshold(String testGroup, String testName, Double delta, boolean ignoreNegativeDeltas){
        super(testGroup, testName, ThresholdTypes.tt_delta);
        this.delta = delta;
        this.ignoreNegativeDeltas = ignoreNegativeDeltas;
        this.previous = null;
    }

    public DeltaThreshold(Double delta, boolean ignoreNegativeDeltas) throws ValidationException {
        super(ThresholdTypes.tt_delta);
        if (delta == null){
            throw new ValidationException(Messages.DeltaThreshold_MissingDeltaValue());
        }
        this.delta = delta;
        this.ignoreNegativeDeltas = ignoreNegativeDeltas;
        this.previous = null;
    }

    // Functions
    public boolean evaluate(List<? extends Run<?, ?>> builds) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, AbortException, ParseException {
        return true;
    }

    @Override
    public boolean isValid(int value) throws NullPointerException, ValidationException {
        if ( previous == null )
            return true;

        double calculatedDelta = value - previous;
        if (calculatedDelta < 0 && this.ignoreNegativeDeltas == true)
            return true;

        double absValueDelta = Math.abs(calculatedDelta);

        if ( delta != null && absValueDelta > delta) {
            throw new ValidationException(Messages.DeltaThreshold_ValueOutOfDeltaFromPrevious(Integer.toString(value), Double.toString(delta), Double.toString(previous)));
        }
        return true;
    }

    @Override
    public boolean isValid(double value) throws NullPointerException, ValidationException {
        if ( previous == null )
            return true;

        double calculatedDelta = value - previous;
        if (calculatedDelta < 0 && this.ignoreNegativeDeltas == true)
            return true;

        double absValueDelta = Math.abs(calculatedDelta);

        if ( delta != null && absValueDelta > delta) {
            throw new ValidationException(Messages.DeltaThreshold_ValueOutOfDeltaFromPrevious(Double.toString(value), Double.toString(delta), Double.toString(previous)));
        }
        return true;
    }

    // Setter
    public void setPreviousValue(Double previousValue){ this.previous = previousValue; }

    // Getter
    public Double getDelta() { return delta; }
    public Boolean getIgnoreNegativeDeltas() { return ignoreNegativeDeltas; }
    public Double getPrevious() { return previous; }

    // Descriptor (active interactor)
    @Extension
    public static class DescriptorImpl extends ThresholdDescriptor {

        @Override
        public String getDisplayName() {return Messages.DeltaThreshold_DeltaFromLastBuild();}

        public FormValidation doCheckDelta(@QueryParameter Double delta) {
            if (delta == null){
                return FormValidation.error(Messages.DeltaThreshold_DeltaCannotBeEmpty());
            }
            if (delta < 0){
                return FormValidation.error(Messages.DeltaThreshold_DeltaAboveOrEqualTo0());
            }
            return FormValidation.ok();
        }
    }
}
