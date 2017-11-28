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

/**
 * Store general status that define the extend of the row headers in the Benchmark tables
 *
 * @author Daniel Mercier
 * @since 6/20/2017.
 */
public class ContentDetected {

    // Variables

    private Boolean       fileDetected;
    private Boolean       groupDetected;
    private Boolean       numeralDetected;
    private Boolean       unitsDetected;

    // Constructor

    public ContentDetected(){
        this.fileDetected = false;
        this.groupDetected = false;
        this.numeralDetected = false;
        this.unitsDetected = false;
    }

    // Setter

    public void setFileDetected(Boolean fileDetected) { this.fileDetected = fileDetected; }
    public void setGroupDetected(Boolean groupDetected) { this.groupDetected = groupDetected; }
    public void setNumeralDetected(Boolean numeralDetected) { this.numeralDetected = numeralDetected; }
    public void setUnitsDetected(Boolean unitsDetected) { this.unitsDetected = unitsDetected; }

    // Getter

    public Boolean isFileDetected() { return fileDetected; }
    public Boolean isGroupDetected() { return groupDetected; }
    public Boolean isNumeralDetected() { return numeralDetected; }
    public Boolean isUnitsDetected() { return unitsDetected; }
}
