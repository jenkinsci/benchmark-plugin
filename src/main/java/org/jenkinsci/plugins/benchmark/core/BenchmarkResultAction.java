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
package org.jenkinsci.plugins.benchmark.core;

import com.google.gson.JsonArray;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Job;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.plugins.benchmark.parsers.MapperBase;
import org.jenkinsci.plugins.benchmark.results.NumeralValue;
import org.jenkinsci.plugins.benchmark.results.TestValue;
import org.jenkinsci.plugins.benchmark.utilities.FrontendMethod;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 *
 * @author Daniel Mercier
 * @since 5/16/2017
 */
public class BenchmarkResultAction implements Action, SimpleBuildStep.LastBuildAction {

    // Variables

    private static final Logger log = Logger.getLogger(BenchmarkResultAction.class.getName());

    private final Job<?, ?> project;
    private final BenchmarkPublisher    core;

    private transient TreeSet<Integer>  builds;
    private transient TestValue         result;

    // Constructor

    BenchmarkResultAction(final Job<?, ?> project, final BenchmarkPublisher core) {
        this.project = project;
        this.core = core;
    }

    // Functions

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return "BenchmarkResult";
    }

    @Override
    public String getDisplayName() {
        return Messages.BenchmarkResultAction_DisplayName();
    }

    /**
     * Get text direction (left to right/right to left)
     * @return rtl or ltr
     */
    @FrontendMethod
    public String getTextDirection() {
        if (ComponentOrientation.getOrientation(Locale.getDefault()).isLeftToRight()){
            return "ltr";
        } else {
            return "rtl";
        }
    }

    /**
     * Get box position from text direction (left to right/right to left)
     * @return right or left
     */
    @FrontendMethod
    public String getRightBoxPosition() {
        if (ComponentOrientation.getOrientation(Locale.getDefault()).isLeftToRight()){
            return "right";
        } else {
            return "left";
        }
    }

    @FrontendMethod
    public Boolean getHasResult(){
        if (this.core.getSelectedResult() == null){
            return false;
        } else {
            return true;
        }
    }

    @FrontendMethod
    public String getResultName() {
        this.result = null;
        try {
            Integer resultID = this.core.getSelectedResult();
            if (resultID != null) {
                this.core.fillAllResults(project);
                MapperBase mapper = this.core.getMapper();
                result = mapper.getResults().get(resultID);
                builds = mapper.getBuilds();
                return Messages.BenchmarkResultAction_ResultName(result.getName());
            } else {
                return "none";
            }
        } catch (Exception e){
            return "none";
        }
    }

    @FrontendMethod
    public String  getGroupName() {
        try {
            MapperBase base = this.core.getMapper();
            if (base != null && base.getDetected().isGroupDetected()) {
                if (result.getGroup() == null) {
                    return Messages.BenchmarkResultAction_NoGroup();
                } else {
                    return Messages.BenchmarkResultAction_GroupName(result.getGroup());
                }
            } else {
                return "none";
            }
        } catch (Exception e) {
            return "none";
        }
    }

    @FrontendMethod
    public String  getFileName(){
        try {
            MapperBase base = this.core.getMapper();
            if (base != null && base.getDetected().isFileDetected()) {
                return Messages.BenchmarkResultAction_FileName(result.getParent().getDescription());
            } else {
                return "none";
            }
        } catch (Exception e) {
            return "none";
        }
    }

    @FrontendMethod
    public Boolean getIsNumeral(){
        try{
            return result.isNumeral();
        } catch (Exception e) {
            return false;
        }
    }

    @FrontendMethod
    public String getGraphTitle(){
        try {
            NumeralValue value = (NumeralValue)this.result;
            return value.getUnit();
        } catch (Exception e) {
            return "none";
        }
    }

    @FrontendMethod
    public JsonArray getChartLabels() {
        JsonArray array = new JsonArray();
        if (ComponentOrientation.getOrientation(Locale.getDefault()).isLeftToRight()) {
            for (Integer build = builds.last(); build >= builds.first(); build--) {
                array.add(build);
            }
        } else {
            for (Integer build = builds.first(); build <= builds.last(); build++) {
                array.add(build);
            }
        }
        return array;
    }

    @FrontendMethod
    public JsonArray getChartData(){
        try {
            return result.getDataAsJsonArray(builds);
        } catch (Exception e) {
            return new JsonArray();
        }
    }

    @FrontendMethod
    public String getTablePageURL(){
        return  Jenkins.get().getRootUrl() + project.getUrl() + "BenchmarkTable";
    }

    @FrontendMethod
    public String getRawTable(){
        try {
            TestValue result = this.result;
            MapperBase base = this.core.getMapper();
            if (base != null && result != null) {
                StringBuffer output = new StringBuffer();
                output.append("<thead><tr><th>");
                output.append(Messages.Build());
                for (Integer build = builds.last(); build >= builds.first(); build--) {
                    output.append("</th><th>");
                    output.append(build.toString());
                }
                output.append("</th></tr></thead><tbody><tr><td style=\"text-align:center;\"><b>");
                output.append(Messages.Value());
                output.append("</b></td>");
                output.append(result.getHTMLResult(builds, base.getDecimalSeparator()));
                output.append("</tr></tbody>");
                return output.toString();
            } else {
                return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    @FrontendMethod
    public String getCondensedTable(){
        try {
            TestValue result = this.result;
            MapperBase base = this.core.getMapper();
            if (base != null && result != null) {
                return result.getHTMLCondensedDetail(base.getDetected(), base.getDecimalSeparator());
            } else {
                return "";
            }
        } catch (Exception e){
            return "";
        }
    }

    @FrontendMethod
    public String getGraphYMinimum(){
        try {
            MapperBase base = this.core.getMapper();
            NumeralValue value =  (NumeralValue)base.getResults().get(this.core.getSelectedResult());
            if (value.getMaximum() > value.getMinimum()) {
                return String.format(Locale.US, "%6g", value.getMinimum() - 0.2 * (value.getMaximum() - value.getMinimum()));
            } else {
                return String.format(Locale.US, "%6g", value.getMaximum() - 0.2 * value.getMaximum());
            }
        } catch (Exception e){
            return "0";
        }
    }

    @FrontendMethod
    public String getGraphYMaximum(){
        try {
            MapperBase base = this.core.getMapper();
            NumeralValue value =  (NumeralValue)base.getResults().get(this.core.getSelectedResult());
            if (value.getMaximum() > value.getMinimum()) {
                return String.format(Locale.US, "%6g", value.getMaximum() + 0.2 * (value.getMaximum() - value.getMinimum()));
            } else {
                return String.format(Locale.US, "%6g", value.getMaximum() + 0.2 * value.getMaximum());
            }
        } catch (Exception e){
            return "10";
        }
    }

    @JavaScriptMethod
    public String getResultDetails(){
        try {
            TestValue result = this.result;
            MapperBase base = this.core.getMapper();
            if (base != null && result != null) {
                return result.getHTMLDetails(getBuildNumber(), base.getDecimalSeparator());
            } else {
                return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    @JavaScriptMethod
    public String getParameters(){
        try {
            TestValue result = this.result;
            MapperBase base = this.core.getMapper();
            if (base != null && result != null) {
                return result.getHTMLParameters(getBuildNumber(), base.getDecimalSeparator());
            } else {
                return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get the absolute address
     * @return Absolute address
     */
    @FrontendMethod
    public String getResultPageURL(){
        return  Jenkins.get().getRootUrl() + project.getUrl() + "BenchmarkResult";
    }

    @JavaScriptMethod
    public int getBuildSelected(){
        Integer build = this.core.getSelectedBuild();
        if (build == null){
            return 1;
        } else {
            return this.core.getSelectedBuild();
        }
    }

    @JavaScriptMethod
    public int getBuildNumber(){
        Integer build = this.core.getSelectedBuild();
        if (build == null){
            return builds.last();
        } else {
            return builds.last() - build + 1;
        }
    }

    /**
     * Reset clock that keeps the result loaded in memory
     */
    @JavaScriptMethod
    public void resetClock(){
        this.core.resetClock();
    }

    @JavaScriptMethod
    public void setBuildSelected(Integer build){
        this.core.setSelectedBuild(build);
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        List<BenchmarkResultAction> projectActions = new ArrayList<>();
        projectActions.add(new BenchmarkResultAction(project, core));
        return projectActions;
    }

    // Getters

    public Job<?, ?> getProject() { return project; }
    public BenchmarkPublisher getCore() { return core; }
}
