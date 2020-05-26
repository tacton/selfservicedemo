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

import java.util.List;

public class Catalog {

    private String name;
    private String title;
    private String type;
    private boolean isConfigurable;
    private boolean isExternalSearch;
    private List<Filter> filters;
    private List<Column> columns;

    public String getName() { return name; }
    public void setName(String value) { this.name = value; }

    public String getTitle() { return title; }
    public void setTitle(String value) { this.title = value; }

    public String getType() { return type; }
    public void setType(String value) { this.type = value; }

    public boolean getIsConfigurable() { return isConfigurable; }
    public void setIsConfigurable(boolean value) { this.isConfigurable = value; }

    public boolean getIsExternalSearch() { return isExternalSearch; }
    public void setIsExternalSearch(boolean value) { this.isExternalSearch = value; }

    public List<Filter> getFilters() { return filters; }
    public void setFilters(List<Filter> value) { this.filters = value; }

    public List<Column> getColumns() { return columns; }
    public void setColumns(List<Column> value) { this.columns = value; }
}
