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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gather associated property to test result.
 *
 * @author Daniel Mercier
 * @since 6/20/2017.
 */
public class TestProperty {

    // Variables

    private Integer                 id;
    private Boolean                 failedState;
    private Map<String, String>     messages;
    private List<TestValue>         parameters;

    // Constructor

    TestProperty(){
        id = null;
        failedState = null;
        messages = new HashMap<String,String>();
        parameters = new ArrayList<TestValue>();
    }

    // Setters

    public void setId(Integer id) { this.id = id; }
    public void setFailedState(Boolean failedState) { this.failedState = failedState; }
    public void addMessage(String title, String message) { this.messages.put(title,message); }
    public void addMessages(Map<String, String> messages) { this.messages.putAll(messages); }
    public void addParameter(TestValue parameter) { this.parameters.add(parameter); }
    public void addParameters(List<TestValue> parameters) { this.parameters.addAll(parameters); }

    // Getters

    public Integer getId() { return id; }
    public Boolean getFailedState() { return failedState; }
    public Map<String, String> getMessages() { return messages; }
    public List<TestValue> getParameters() { return parameters; }
}
