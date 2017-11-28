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
package org.jenkinsci.plugins.benchmark.results;

import javax.xml.bind.ValidationException;

/**
 * Holds definition of failure for boolean test
 *
 * @author Daniel Mercier
 * @since 5/10/2017
 */
public class TestFailure {

    // Enumerations

    public enum FailureType{
        ftBoolean,
        ftInteger,
        ftValueString,
        ftKeyString
    }

    public enum CompareType{
        ct_below,
        ct_belowOrEqual,
        ct_equal,
        ct_aboveOrEqual,
        ct_above
    }

    // Variables

    private final FailureType   type;
    private final Boolean       boolValue;
    private final Double        dblValue;
    private final String        strValue;
    private final CompareType   compType;

    //Constructors

    public TestFailure(boolean value){
        this.type = FailureType.ftBoolean;
        this.boolValue = value;
        this.dblValue = null;
        this.strValue = null;
        this.compType = CompareType.ct_equal;
    }

    public TestFailure(double value){
        this.type = FailureType.ftInteger;
        this.boolValue = null;
        this.dblValue = value;
        this.strValue = null;
        this.compType = CompareType.ct_equal;
    }

    public TestFailure(double value, String compareType) throws ValidationException {
        this.type = FailureType.ftInteger;
        this.boolValue = null;
        this.dblValue = value;
        this.strValue = null;
        if (compareType == null) {
            this.compType = CompareType.ct_equal;
        } else {
            compareType = compareType.toLowerCase();
            if (compareType.equals("above"))
                this.compType = CompareType.ct_above;
            else if (compareType.equals("below"))
                this.compType = CompareType.ct_below;
            else if (compareType.equals("aboveorequal"))
                this.compType = CompareType.ct_aboveOrEqual;
            else if (compareType.equals("beloworequal"))
                this.compType = CompareType.ct_belowOrEqual;
            else
                throw new ValidationException(Messages.TestFailure_CompareTypeUnknown());
        }
    }

    public TestFailure(String value){
        this.type = FailureType.ftValueString;
        this.boolValue = null;
        this.dblValue = null;
        this.strValue = value;
        this.compType = CompareType.ct_equal;
    }

    public TestFailure(String value, boolean key){
        if (key)
            this.type = FailureType.ftKeyString;
        else
            this.type = FailureType.ftValueString;
        this.boolValue = null;
        this.dblValue = null;
        this.strValue = value;
        this.compType = CompareType.ct_equal;
    }

    // Functions

    public boolean isFailure(boolean content){
        if (this.boolValue != null) {
            if (content == this.boolValue.booleanValue())
                return true;
        }
        return false;
    }

    public boolean isFailure(double content) {
        if (this.dblValue != null) {
            switch (compType) {
                case ct_equal:
                    if (content == this.dblValue.doubleValue())
                        return true;
                    break;
                case ct_aboveOrEqual:
                    if (content >= this.dblValue.doubleValue())
                        return true;
                    break;
                case ct_belowOrEqual:
                    if (content <= this.dblValue.doubleValue())
                        return true;
                    break;
                case ct_above:
                    if (content > this.dblValue.doubleValue())
                        return true;
                    break;
                case ct_below:
                    if (content < this.dblValue.doubleValue())
                        return true;
            }
        }
        return false;
    }

    public boolean isFailure(int content){
        if (this.dblValue != null) {
            switch (compType) {
                case ct_equal:
                    if (content == this.dblValue.doubleValue())
                        return true;
                    break;
                case ct_aboveOrEqual:
                    if (content >= this.dblValue.doubleValue())
                        return true;
                    break;
                case ct_belowOrEqual:
                    if (content <= this.dblValue.doubleValue())
                        return true;
                    break;
                case ct_above:
                    if (content > this.dblValue.doubleValue())
                        return true;
                    break;
                case ct_below:
                    if (content < this.dblValue.doubleValue())
                        return true;
            }
        }
        return false;
    }

    public boolean isFailure(String content){
        if (this.strValue != null) {
            if (content.equals(this.strValue))
                return true;
        }
        return false;
    }

    public boolean isFailure(String content, boolean key){
        if (this.strValue != null && isKeyFailure()) {
            if (content.equals(this.strValue))
                return true;
        }
        return false;
    }


    // Getter

    public boolean isKeyFailure(){ return (this.type == FailureType.ftKeyString); }
}
