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
package org.jenkinsci.plugins.benchmark.schemas;

/**
 * Base class to define internally available resource schema(s)
 * @author Daniel Mercier
 * @since 5/16/2017
 */
public class Schema {

    // Enumeration

    public static final int No_format   = 1 << 0;
    public static final int Xml_format  = 1 << 1;
    public static final int Json_format = 1 << 2;

    // Variable

    private final String        name;
    private final String        location;
    private final String        description;
    private final int           format;

    // Constructor

    public Schema(String name, String location, String description, int format){
        this.name = name;
        this.location = location;
        this.description = description;
        this.format = format;

    }

    // Getter

    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getDescription() { return description; }
    public String getDisplayName() {
        if (description == null || description.length() == 0) {
            return name;
        } else {
            return name + " - " + description;
        }
    }
    public String getFormat() {
        String value = "";
        if ((format & Schema.Xml_format) == Schema.Xml_format) {
            value += "XML";
        }
        if ((format & Schema.Json_format) == Schema.Json_format) {
            if (!value.isEmpty()) {
                value += "|";
            }
            value += "JSON";
        }
        return value;
    }
}
