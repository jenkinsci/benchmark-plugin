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

import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.QueryParameter;

/**
 * Threshold descriptor
 *
 * @author Daniel Mercier
 * @since 5/17/2017.
 */
public abstract class ThresholdDescriptor extends Descriptor<Threshold> {

    // Functions
    public final String getId() {
        return getClass().getName();
    }

    public static DescriptorExtensionList<Threshold, ThresholdDescriptor> all() {
        return Jenkins.get().getDescriptorList(Threshold.class);
    }

    public static ThresholdDescriptor getById(String id) {
        for (ThresholdDescriptor d : all())
            if (d.getId().equals(id))
                return d;
        return null;
    }

    public FormValidation doCheckTestName(@QueryParameter String testName) {
        if (!testName.isEmpty() && testName.contains(" ")) {
            return FormValidation.error(Messages.ThresholdDescriptor_ResultCannotHaveSpace());
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckTestGroup(@QueryParameter String testGroup) {
        if (!testGroup.isEmpty() && testGroup.contains(" ")) {
            return FormValidation.error(Messages.ThresholdDescriptor_GroupCannotHaveSpace());
        }
        return FormValidation.ok();
    }
}
