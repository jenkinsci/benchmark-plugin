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
package org.jenkinsci.plugins.benchmark.utilities;

import java.io.File;

/**
 * File address storage and processor for jUnitJenkins mapper
 *
 * @author Daniel Mercier
 * @since 6/20/2017.
 */
public class FilePathGroup {

    // Variables

    String          relativePath;
    String[]        pathSegment;
    StringBuffer    fullPath;


    // Constructor

    public FilePathGroup(String fullpath){
        this.pathSegment = fullpath.split("\\\\|\\/");
        this.fullPath = new StringBuffer();
        FilePathToString();
    }

    /**
     * Convert FilePath to String
     */
    private void FilePathToString(){
        boolean detectWorkspace = false;
        boolean passedFirst = false;
        for(String segment:pathSegment){
            if(!detectWorkspace) {
                if (segment.equalsIgnoreCase("workspace")) {
                    detectWorkspace = true;
                }
            } else {
                if (passedFirst){
                    this.fullPath.append(File.separator);
                    this.fullPath.append(segment);
                } else {
                    this.fullPath.append(segment);
                    passedFirst = true;
                }
            }
        }
    }

    // Setter

    public void setRelativepath(String relativePath) { this.relativePath = relativePath; }

    // Getter

    public String getRelativePath() { return this.relativePath; }
    public int getNumberOfSegments() { return this.pathSegment.length; }
    public String getPathSegment(int i) { return this.pathSegment[i]; }
    public String getFullPath() { return this.fullPath.toString(); }
}
