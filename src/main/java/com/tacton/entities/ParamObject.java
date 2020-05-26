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

package com.tacton.entities;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ParamObject {
    private Map<String, String> values = new HashMap<>();

    @JsonAnySetter
    public void put(String property, String value) {
        values.put(property, value);
    }

    @JsonAnyGetter
    public Map<String, String> get() {
        return values;
    }

    public void mapParams(Map<String, Supplier<String>> paramMapping ) {
        values.replaceAll( (key, value) -> {
            Supplier<String> supplier = paramMapping.get(value);
            if(supplier != null) {
                value = supplier.get();
            }
            return value;
        });
    }

    public Optional<Supplier<Stream<String>>> choseSupplier(Map<String, Supplier<Stream<String>>> paramMapping ) {
        return values.entrySet().stream()
            .filter( entry -> paramMapping.containsKey(entry.getValue()))
            .map( entry -> paramMapping.get(entry.getValue()))
            .findAny();
    }
}
