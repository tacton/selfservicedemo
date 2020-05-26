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

package com.tacton.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tacton.entities.ParamObject;
import com.tacton.entities.cpqresponse.CartAttributes;
import com.tacton.entities.cpqresponse.Country;
import com.tacton.entities.cpqresponse.Currency;
import com.tacton.services.cpq.CartService;
import com.tacton.services.cpq.CountryService;
import com.tacton.services.cpq.CurrencyService;
import com.tacton.services.cpq.WrongAccountException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Service
public class NeedsParam {

    @Autowired
    private CartService cartService;

    @Autowired
    private CountryService countryService;

    @Autowired
    private CurrencyService currencyService;


    public String prepareCartNeedsParams(String needsParam, String cartId) throws IOException {
        final CartAttributes shoppingCart = cartService.getCartFromCPQapi(cartId);
        if(shoppingCart == null) {
            throw new WrongAccountException("wrongCartMessage");
        }

        Supplier<String> countryName = () -> {
            Country country = countryService.getCountry(shoppingCart.getInstallationCountry());
            if (country == null) {
                throw new WrongAccountException("wrongCartMessage");
            }
            return country.getName();
        };

        Supplier<String> currencyIso = () -> {
            Currency currency = currencyService.getCurrency(shoppingCart.getCurrency());
            if (currency == null) {
                throw new WrongAccountException("wrongCartMessage");
            }
            return currency.getIsoCode();
        };

        ObjectMapper objectMapper = new ObjectMapper();
        ParamObject paramObject = objectMapper.readValue(needsParam, ParamObject.class);

        paramObject.mapParams(Map.of("$cart.installationCountry", countryName,
                "$cart.currency", currencyIso));

        return objectMapper.writeValueAsString(paramObject);
    }

    public String prepareLeadGenNeedsParams(String needsParam, Supplier<String> valueSupplier) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        ParamObject paramObject = objectMapper.readValue(needsParam, ParamObject.class);

        paramObject.mapParams(Map.of("$select.country", valueSupplier,
                "$select.currency", valueSupplier));

        return objectMapper.writeValueAsString(paramObject);
    }

    public Optional<Supplier<Stream<String>>> getUsedList(String needsParam) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        ParamObject paramObject = objectMapper.readValue(needsParam, ParamObject.class);

        return paramObject.choseSupplier(Map.of("$select.country", this::countryList,
                "$select.currency", this::currencyList));
    }

    private Stream<String> countryList() {
        return  countryService.listCountries()
                .stream()
                .map(Country::getName);
    }

    private Stream<String> currencyList() {
        return  currencyService.listCurrencies()
                .stream()
                .map(Currency::getIsoCode);
    }

}
