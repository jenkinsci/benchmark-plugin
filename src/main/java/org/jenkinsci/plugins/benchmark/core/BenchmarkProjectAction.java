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

import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Api;
import hudson.model.Run;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.benchmark.utilities.*;
import org.jenkinsci.plugins.benchmark.parsers.MapperBase;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.awt.*;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Job action = icon on the left menu
 *
 * @author Daniel Mercier
 * @since 5/16/2017.
 */
@ExportedBean
public class BenchmarkProjectAction implements Action{

    // Variables

    private static final Logger log = Logger.getLogger(BenchmarkProjectAction.class.getName());

    private final AbstractProject<?, ?>     project;
    private final BenchmarkPublisher        core;

    // Constructor

    BenchmarkProjectAction(final AbstractProject<?, ?> project,  BenchmarkPublisher core) {
        this.project = project;
        this.core = core;
    }

    // Function overrides

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return "BenchmarkTable";
    }

    // Functions

    /**
     * Get text direction (left to right/right to left)
     * @return rtl or ltr
     */
    @FrontendMethod
    public String getTextDirection() {
        Locale locale = Locale.getDefault();
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
        Locale locale = Locale.getDefault();
        if (ComponentOrientation.getOrientation(Locale.getDefault()).isLeftToRight()){
            return "right";
        } else {
            return "left";
        }
    }

    /**
     * Get box position from text direction (left to right/right to left)
     * @return right or left
     */
    @FrontendMethod
    public String getBottomMessage() {
        return  Messages.BenchmarkProjectAction_DownloadTablesAt();
    }

    /**
     * Get the absolute address
     * @return Absolute address
     */
    @FrontendMethod
    public String getRootPageURL(){
        return  Jenkins.getInstance().getRootUrl() + project.getUrl();
    }

    /**
     * Get the absolute address
     * @return Absolute address
     */
    @FrontendMethod
    public String getResultPageURL(){
        return  Jenkins.getInstance().getRootUrl() + project.getUrl() + "BenchmarkResult";
    }

    /**
     * Get the API address
     * @return Api address
     */
    @FrontendMethod
    public String getAPIURL(){
        return  Jenkins.getInstance().getRootUrl() + project.getUrl() + "BenchmarkTable/api/json";
    }

    /**
     * Identify whether results are available
     * @return Whether content is available
     */
    @FrontendMethod
    public Boolean getContentAvailable() {
        try {
            Run run = this.project.getLastBuild();
            if (this.core.hasResults(run)) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Provide number of headers
     * @return Number of row heads
     */
    @FrontendMethod
    public int getNumberOfHeads(){
        int i = 1;
        try {
            Run run = project.getLastBuild();
            if (run != null) {
                MapperBase base = this.core.getMapper(run);
                ContentDetected detected = base.getDetected();
                if (detected != null) {
                    if (detected.isFileDetected()) {
                        ++i;
                    }
                    if (detected.isGroupDetected()) {
                        ++i;
                    }
                    if (detected.isUnitsDetected()) {
                        ++i;
                    }
                }
            }
            return i;
        } catch(Exception e) {
            return i;
        }
    }

    /**
     * Get the HTML raw content
     * @return HTML raw content
     */
    @FrontendMethod
    public String getRawTable() {
        try {
            resetClock();
            Run run = project.getLastBuild();
            if (run != null) {
                MapperBase base = this.core.getMapper(run);
                return base.getHTMLTable();
            } else {
                return "";
            }
        } catch (Exception e) {
            log.info(Messages.BenchmarkProjectAction_RawTableErrorDetected());
            log.info(Messages.BenchmarkProjectAction_RawTableErrorMessage(e.getMessage()));
            return "";
        }
    }

    /**
     * Get the HTML condensed content
     * @return HTML condensed content
     */
    @JavaScriptMethod
    public String getCondensedTable(){
        try {
            Run run = project.getLastBuild();
            if (run != null) {
                MapperBase base = this.core.getMapper(run);
                return base.getHTMLCondensedTable();
            } else {
                return "";
            }
        } catch (Exception e) {
            log.info(Messages.BenchmarkProjectAction_CondensedTableErrorDetected());
            log.info(Messages.BenchmarkProjectAction_CondensedTableErrorMessage(e.getMessage()));
            return "";
        }
    }

    @FrontendMethod
    public String getEmptyTableWord(){
        return Messages.BenchmarkProjectAction_DatatablesEmptyTable();
    }

    @FrontendMethod
    public String getZeroRecordsWord(){
        return Messages.BenchmarkProjectAction_DatatablesZeroRecords();
    }

    @FrontendMethod
    public String getFirstWord(){
        return Messages.BenchmarkProjectAction_DatatablesFirst();
    }

    @FrontendMethod
    public String getPreviousWord(){
        return Messages.BenchmarkProjectAction_DatatablesPrevious();
    }

    @FrontendMethod
    public String getNextWord(){
        return Messages.BenchmarkProjectAction_DatatablesNext();
    }

    @FrontendMethod
    public String getLastWord(){
        return Messages.BenchmarkProjectAction_DatatablesLast();
    }

    @JavaScriptMethod
    public void setResultSelected(Integer result){
        this.core.setSelectedResult(result);
        this.core.setSelectedBuild(null);
    }

    /**
     * Reset clock that keeps the result loaded in memory
     */
    @JavaScriptMethod
    public void resetClock(){
        this.core.resetClock();
    }

    /**
     * Get the CSV raw content
     * @return CSV raw content
     */
    @JavaScriptMethod
    public String getCSVRawTable() {
        try {
            resetClock();
            Run run = project.getLastBuild();
            if (run != null) {
                MapperBase base = this.core.getMapper(run);

                StringBuffer output = new StringBuffer();
                output.append(base.getCSVTableHeader());
                output.append("\n");
                output.append(base.getCSVTableBody());
                return output.toString();
            } else {
                return "";
            }
        } catch (Exception e) {
            log.info(Messages.BenchmarkProjectAction_CsvRawTableErrorDetected());
            log.info(Messages.BenchmarkProjectAction_CsvRawTableErrorMessage(e.getMessage()));
            return "";
        }
    }

    @Exported(visibility=2)
    public String getCSVRawHeader() {
        try {
            resetClock();
            Run run = project.getLastBuild();
            if (run != null) {
                MapperBase base = this.core.getMapper(run);
                return base.getCSVTableHeader() ;
            } else {
                return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    @Exported(visibility=2)
    public String getCSVRawBody() {
        try {
            resetClock();
            Run run = project.getLastBuild();
            if (run != null) {
                MapperBase base = this.core.getMapper(run);
                return base.getCSVTableBody() ;
            } else {
                return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get the CSV raw content
     * @return CSV raw content
     */
    @JavaScriptMethod
    public String getCSVRawStateTable() {
        try {
            resetClock();
            Run run = project.getLastBuild();
            if (run != null) {
                MapperBase base = this.core.getMapper(run);

                String output = "";
                output += base.getCSVTableHeader() + "\n";
                output += base.getCSVTableStateBody();
                return output;
            } else {
                return "";
            }
        } catch (Exception e) {
            log.info("Benchmark CSV Raw Table Result - Error detected");
            log.info("Benchmark CSV Raw Table Result - " + e.getMessage());
            return "";
        }
    }

    @Exported(visibility=2)
    public String getCSVRawStateBody() {
        try {
            resetClock();
            Run run = project.getLastBuild();
            if (run != null) {
                MapperBase base = this.core.getMapper(run);
                return base.getCSVTableStateBody() ;
            } else {
                return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get the CSV condensed content
     * @return CSV condensed content
     */
    @JavaScriptMethod
    public String getCSVCondensedTable(){
        try {
            Run run = project.getLastBuild();
            if (run != null) {
                MapperBase base = this.core.getMapper(run);

                StringBuffer output = new StringBuffer();
                output.append(base.getCSVCondensedTableHeader());
                output.append("\n");
                output.append(base.getCSVCondensedTableBody());
                return output.toString();
            } else {
                return "";
            }
        } catch (Exception e) {
            log.info(Messages.BenchmarkProjectAction_CsvCondensedTableErrorDetected());
            log.info(Messages.BenchmarkProjectAction_CsvCondensedTableErrorMessage(e.getMessage()));
            return "";
        }
    }

    @Exported(visibility=2)
    public String getCSVCondensedHeader(){
        try {
            Run run = project.getLastBuild();
            if (run != null) {
                MapperBase base = this.core.getMapper(run);
                return base.getCSVCondensedTableHeader();
            } else {
                return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    @Exported(visibility=2)
    public String getCSVCondensedBody(){
        try {
            Run run = project.getLastBuild();
            if (run != null) {
                MapperBase base = this.core.getMapper(run);
                return base.getCSVCondensedTableBody();
            } else {
                return "";
            }
        } catch (Exception e) {
            return "";
        }
    }


    /**
     * Exposes this object to the remote API.
     * @return Construct that displays this class content.
     */
    public Api getApi() {
        return new Api(this);
    }

    // Getters

    public AbstractProject<?, ?> getProject() { return project; }
    public BenchmarkPublisher getCore() { return core; }
}
