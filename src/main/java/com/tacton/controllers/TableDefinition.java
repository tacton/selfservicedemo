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

package com.tacton.controllers;

import com.tacton.entities.cpqresponse.CpqEntity;
import org.apache.commons.lang3.StringUtils;

import java.text.NumberFormat;
import java.util.*;
import java.util.function.Function;

public class TableDefinition {
    private List<String> fields;
    private List<String> totals;
    private Set<String> percentFields;
    private Set<String> currencyFields;
    private NumberFormat percentFormatter;
    private NumberFormat currencyFormatter;
    private Function<CpqEntity, String> currencyProvider;

    public TableDefinition(String[] fields, Function<CpqEntity, String> currencyProvider,
                           String[] percentFields, String[] currencyFields) {
        Locale locale = Locale.ENGLISH;
        this.percentFormatter = NumberFormat.getNumberInstance(locale);
        this.percentFormatter.setMaximumFractionDigits(2);

        this.currencyFormatter = NumberFormat.getCurrencyInstance(locale);
        this.currencyFormatter.setMaximumFractionDigits(2);

        this.percentFields = new HashSet<>(Arrays.asList(percentFields));
        this.currencyFields = new HashSet<>(Arrays.asList(currencyFields));

        this.fields =  Arrays.asList(fields);

        this.currencyProvider = currencyProvider;
    }

    public TableDefinition(String[] fields, String[] totals, Function<CpqEntity, String> currencyProvider,
                           String[] percentFields, String[] currencyFields) {
        this(fields, currencyProvider, percentFields, currencyFields);
        this.totals =  Arrays.asList(totals);
    }

    public String getValue(CpqEntity entity, String property) {
        String value = entity.get(property);
        if(!StringUtils.isEmpty(value)) {
            String actCurr;
            if(percentFields.contains(property)) {
                value = percentFormatter.format(Double.valueOf(value)) + " %";
            }
            else
            if(currencyFields.contains(property) && (actCurr = currencyProvider.apply(entity)) != null) {
                Currency curr = Currency.getInstance(actCurr);
                currencyFormatter.setCurrency(curr);

                value = currencyFormatter.format(Double.valueOf(value));
            }
        }
        return value;
    }

    public int getColumns() {
        return fields.size();
    }

    public List<String> getFields() {
        return fields;
    }

    public List<String> getTotals() {
        return totals;
    }
}
