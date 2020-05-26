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

package com.tacton.services.cpq;

import com.tacton.entities.cpqresponse.Account;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class AccountService {

    @Value("${cpq_api_url}")
    private String cpq_api_url;

    @Value("${cpq_user}")
    private String cpq_user;

    @Value("${cpq_pass}")
    private String cpq_pass;

    private final Logger LOGGER = LoggerFactory.getLogger(AccountService.class);

    public Account getAccount(String accountId) {

        HttpHeaders headers = getAuthenticationHeaders(cpq_user, cpq_pass);

        HttpEntity<String> httpEntity = new HttpEntity<>("body", headers);
        RestTemplate template = new RestTemplate();
        ResponseEntity<String> response;
        try {
             response = template.exchange(
                cpq_api_url + "/account/" + accountId,
                HttpMethod.GET,
                httpEntity,
                String.class);
        } catch(HttpClientErrorException exception ) {
            LOGGER.warn("Account not found: "+accountId, exception);
            return null;
        }

        Document doc = Jsoup.parse(response.getBody());
        Account account = new Account();
        doc.select("attributes").forEach(attributes -> {
            attributes.select("attribute").forEach(attribute -> {
                try {
                    if (PropertyUtils.isWriteable(account, attribute.attr("name").toString())) {
                        BeanUtils.setProperty(account,
                                attribute.attr("name").toString(),
                                attribute.attr("value").toString()
                        );
                    }
                } catch (Exception e) {
                    LOGGER.error("Error parsing results from CPQ",e);
                }
            });
        });

        return account;
    }

    public List<Account> listAccounts() {
        HttpHeaders headers = getAuthenticationHeaders(cpq_user, cpq_pass);

        HttpEntity<String> httpEntity = new HttpEntity<>("body", headers);
        RestTemplate template = new RestTemplate();
        ResponseEntity<String> response = template.exchange(
                cpq_api_url + "/account/list?where=isCustomerSelfServiceEnabled=true&limit=1000",
                HttpMethod.GET,
                httpEntity,
                String.class);

        Document doc = Jsoup.parse(response.getBody());
        List<Account> accounts = new ArrayList<>();


        doc.select("resource").forEach(resource -> {
            Account account = new Account();
            account.setAccountId(resource.attr("id"));
            resource.select("attributes").forEach(attributes -> {
                attributes.select("attribute").forEach(attribute -> {
                    if(!attribute.attr("name").toString().equals("accountId")) {
                        try {
                            if (PropertyUtils.isWriteable(account, attribute.attr("name").toString())) {
                                BeanUtils.setProperty(account,
                                        attribute.attr("name").toString(),
                                        attribute.attr("value").toString()
                                );
                            }
                        } catch (Exception e) {
                            LOGGER.error("Error parsing results from CPQ",e);
                        }
                    }
                });
            });
            accounts.add(account);
        });

        return accounts;
    }

    private HttpHeaders getAuthenticationHeaders(String username, String password) {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization",authHeader);
        return headers;
    }
}
