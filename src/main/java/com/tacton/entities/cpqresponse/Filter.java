/*
 * The MIT License
 *
 * Copyright 2020 Tacton Systems AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tacton.entities.cpqresponse;

import java.util.*;

public class Filter {
    private String name;
    private String title;
    private String widget;
    private String operator;
    private String type;
    private Object parentType;
    private Object parentValue;
    private Object userValue;
    private boolean hidden;
    private boolean isList;
    private boolean hasOptions;
    private List<Object> options;

    public String getName() { return name; }
    public void setName(String value) { this.name = value; }

    public String getTitle() { return title; }
    public void setTitle(String value) { this.title = value; }

    public String getWidget() { return widget; }
    public void setWidget(String value) { this.widget = value; }

    public String getOperator() { return operator; }
    public void setOperator(String value) { this.operator = value; }

    public String getType() { return type; }
    public void setType(String value) { this.type = value; }

    public Object getParentType() { return parentType; }
    public void setParentType(Object value) { this.parentType = value; }

    public Object getParentValue() { return parentValue; }
    public void setParentValue(Object value) { this.parentValue = value; }

    public Object getUserValue() { return userValue; }
    public void setUserValue(Object value) { this.userValue = value; }

    public boolean getHidden() { return hidden; }
    public void setHidden(boolean value) { this.hidden = value; }

    public boolean getIsList() { return isList; }
    public void setIsList(boolean value) { this.isList = value; }

    public boolean getHasOptions() { return hasOptions; }
    public void setHasOptions(boolean value) { this.hasOptions = value; }

    public List<Object> getOptions() { return options; }
    public void setOptions(List<Object> value) { this.options = value; }
}
