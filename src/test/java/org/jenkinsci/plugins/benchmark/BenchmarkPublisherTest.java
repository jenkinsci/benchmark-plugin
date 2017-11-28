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
package org.jenkinsci.plugins.benchmark;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.BatchFile;
import org.jenkinsci.plugins.benchmark.core.BenchmarkPublisher;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import hudson.tasks.Shell;

import java.io.File;

/**
 * Plugin test including the full workflow.
 * @ref https://wiki.jenkins.io/display/JENKINS/Unit+Test
 *
 * @author Daniel Mercier
 * @since 6/20/2017
 */
public class BenchmarkPublisherTest {

    private static String OS = System.getProperty("os.name").toLowerCase();

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void first() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();

        // Add fake result file to workspace
        String text = "{"
                + "     \"name\": \"result_1\","
                + "     \"description\": \"description_res_1\","
                + "     \"value\": false"
                + "}";
        if (isUnix()) {
            project.getBuildersList().add(new Shell("echo " + text + " > result.json"));
        } else {
            project.getBuildersList().add(new BatchFile("echo " + text + " > result.json"));
        }

        // Activate the plugin
        project.getPublishersList().add(new BenchmarkPublisher("result.json", "simplestSchema", true, "", ""));

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");

        String outputfilename = build.getRootDir().getAbsolutePath() + File.separator + "BenchmarkResult.json" ;
        File file = new File(outputfilename);
        assert(file.exists());
    }

    public static boolean isMac() { return (OS.indexOf("mac") >= 0 || OS.indexOf("darwin") >= 0); }
    public static boolean isWindows() { return (OS.indexOf("win") >= 0); }
    public static boolean isUnix() { return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0 ); }
    public static boolean isSolaris() { return (OS.indexOf("sunos") >= 0); }

}