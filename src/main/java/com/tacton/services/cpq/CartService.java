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

import com.tacton.entities.User;
import com.tacton.entities.cpqresponse.*;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    AccountService accountService;

    @Autowired
    CurrencyService currencyService;

    @Value("${customer_self_service_api_key}")
    private String api_key;

    @Value("${customer_self_service_api_url}")
    private String api_url;

    @Value("${cpq_api_url}")
    private String cpq_api_url;

    @Value("${cpq_user}")
    private String cpq_user;

    @Value("${cpq_pass}")
    private String cpq_pass;

    @Value("${cart_limit}")
    private String cartLimit;

    private final Logger LOGGER = LoggerFactory.getLogger(CartService.class);

    private static String CART_ATTRIBUTES_URI = "/cart";

    private static String CART_ITEMS_URI = "/cart/items";

    private static String PROPOSAL_DRAFT_URI = "/proposal/draft";

    private static String REQUEST_FIRM_PROPOSAL_URI = "/proposal/firm-requests";

    private static String CPQ_GETSHOPPINGCARTS_URI = "/shoppingcart/list";

    private static String API_KEY_PARAM = "?_key={api_key}";

    private static String EXTERNAL_ID_PARAM = "&_externalId={external_id}";

    private static String ACCOUNT_PARAM = "&account={account}";

    private static String CURRENCY_PARAM = "&currency={currency}";

    private static String INSTALLATION_COUNTRY_PARAM = "&installationCountry={installationCountry}";

    private static String REFERENCE_EXTERNAL_USER_PARAM = "&referenceExternalUser={referenceExternalUser}";

    private static String CART_LIMIT = "&limit={limit}";

    static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    public List<CartAttributes> findAllCarts() {
        HttpHeaders headers = getAuthenticationHeaders(cpq_user, cpq_pass);

        HttpEntity<String> httpEntity = new HttpEntity<>("body", headers);
        RestTemplate template = new RestTemplate();

        String url = cpq_api_url + CPQ_GETSHOPPINGCARTS_URI + "?includeDeleted=false&limit=1000";
        ResponseEntity<String> response = template.exchange(
                url,
                HttpMethod.GET,
                httpEntity,
                String.class);

        List<CartAttributes> allCartAttributes = parseCartAttributesFromResponse(response);

        return allCartAttributes;
    }


    public List<CartAttributes> findUserCarts(String externalUserId){

        HttpHeaders headers = getAuthenticationHeaders(cpq_user, cpq_pass);

        HttpEntity<String> httpEntity = new HttpEntity<>("body", headers);
        RestTemplate template = new RestTemplate();
        String url = cpq_api_url + CPQ_GETSHOPPINGCARTS_URI + "?where=referenceExternalUser=" + externalUserId
                + "&includeDeleted=false" + CART_LIMIT;

        LOGGER.debug("find user carts url=" + url);

        ResponseEntity<String> response = template.exchange(
                url,
                HttpMethod.GET,
                httpEntity,
                String.class,
                cartLimit);

        List<CartAttributes> allCartAttributes = parseCartAttributesFromResponse(response);

        return allCartAttributes;
    }


    public CartAttributes getCartFromCPQapi(String externalId){
        HttpHeaders headers = getAuthenticationHeaders(cpq_user, cpq_pass);

        HttpEntity<String> httpEntity = new HttpEntity<>("body", headers);
        RestTemplate template = new RestTemplate();
        String url = cpq_api_url + CPQ_GETSHOPPINGCARTS_URI + "?where=externalId=" + externalId + CART_LIMIT;
        ResponseEntity<String> response = template.exchange(
                url,
                HttpMethod.GET,
                httpEntity,
                String.class,
                cartLimit);

        List<CartAttributes> allCarts = parseCartAttributesFromResponse(response);
        List<CartAttributes> openCarts = filterCartsCreated(allCarts);
        return openCarts.size() > 0 ? openCarts.get(0) : null;
    }


    private <T extends CpqEntity> T parseFromElement(Element resource, Function<Element, T> supplier){
        T t = supplier.apply(resource);
        resource.select("attribute").forEach(attribute -> {
            try {
                if (PropertyUtils.isWriteable(t, attribute.attr("name").toString())) {
                    BeanUtils.setProperty(t,
                            attribute.attr("name").toString(),
                            attribute.attr("value").toString());
                } else {
                        t.put(attribute.attr("name").toString(),
                                attribute.attr("value").toString());
                }
            } catch (Exception e) {
                LOGGER.error("Error parsing results from CPQ", e);
            }
        });
        return t;
    }


    private <T extends CpqEntity> List<T> parseFromResponse(ResponseEntity<String> response, Function<Element, T> supplier,
                                                            Consumer<T> objConsumer) {
        Document doc = Jsoup.parse(response.getBody());
        List<T> allObjects = new ArrayList<>();

        doc.select("resource").forEach(resource -> {
            T t = parseFromElement(resource, supplier);
            objConsumer.accept(t);
            allObjects.add(t);
        });

        return allObjects;
    }


    private List<CartAttributes> parseCartAttributesFromResponse(ResponseEntity<String> response) {
        return parseFromResponse(response,
                resource -> {
                    CartAttributes cartAttributes = new CartAttributes();
                    cartAttributes.setState(resource.attr("state"));
                    cartAttributes.setCpqId(resource.attr("id"));
                    Date d = parseDate(resource.attr("modifiedTime"));
                    cartAttributes.setLastModified(d);
                    return cartAttributes;
                },
                cart -> {}
        );
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

    public CartAttributes getCartAttributes(String externalId) {
        RestTemplate template = new RestTemplate();
        return template.getForObject(api_url + CART_ATTRIBUTES_URI + API_KEY_PARAM + EXTERNAL_ID_PARAM, CartAttributes.class, api_key, externalId);
    }

    public void setCartAttributes(String externalId, CartAttributes attributes) {
        RestTemplate template = new RestTemplate();

        String url = api_url + "/cart";

        UriComponentsBuilder builder = null;
        try {
            builder = UriComponentsBuilder.fromUriString(url)
                    // Add query parameter
                    .queryParam("_key", api_key)
                    .queryParam("_externalId", externalId)
                    .queryParam("account", attributes.getAccount())
                    .queryParam("currency", attributes.getCurrency())
                    .queryParam("referenceExternalUser", attributes.getReferenceExternalUser())
                    .queryParam("installationCountry", attributes.getInstallationCountry())
                    .queryParam("customName", URLEncoder.encode(attributes.getCustomName(),"UTF-8"))
                    .queryParam("name", URLEncoder.encode(attributes.getName(),"UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        UriComponents components = builder.build(true);
        URI uri = components.toUri();

        template.put(uri,null);
    }


    public byte[] getDraftProposal(String externalId) throws IOException {
        RestTemplate template = new RestTemplate();
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("X-Key", api_key);
        HttpEntity<String> request = new HttpEntity<String>(requestHeaders);
        ResponseEntity<Resource> response = template.exchange(api_url + PROPOSAL_DRAFT_URI + API_KEY_PARAM + EXTERNAL_ID_PARAM, HttpMethod.GET, request, Resource.class, api_key, externalId);

        //Saving file from response
        InputStream stream = response.getBody().getInputStream();
        byte[] buffer = stream.readAllBytes();
        stream.close();

        return buffer;
    }


    private Date parseDate(String docDate) {
        Date d = null;
        try {
            d = CartService.DATE_FORMAT.parse(docDate);
        } catch (ParseException e) {
            LOGGER.error("Error parsing response from CPQ",e);
        }
        return d;
    }


    public RequestFirmProposalResponse requestFirmProposal(String externalId) {
        RestTemplate template = new RestTemplate();
        return template.postForObject(api_url + REQUEST_FIRM_PROPOSAL_URI + API_KEY_PARAM + EXTERNAL_ID_PARAM,null,
                RequestFirmProposalResponse.class, api_key, externalId);
    }


    public List<FirmProposalRequest> getAllFirmProposalRequestsForCarts(List<CartAttributes> cartsAll){
        List<FirmProposalRequest> firmProposalRequestList = new ArrayList<>();

        for (CartAttributes cart : cartsAll) {
            if(cart.getState().equals("Firm Proposal Requested")) {
                FirmProposalRequest firmProposalRequest = getFirmProposalsForCart(cart.getCpqId());

                if (firmProposalRequest != null) {
                    if (!firmProposalRequestList.contains(firmProposalRequest)) {
                        firmProposalRequest.setCartName(cart.getCustomName());
                        firmProposalRequest.setItemCount(cart.getCnt());

                        firmProposalRequest.setCurrency(cart.getCurrency());
                        firmProposalRequest.copyFrom(cart);

                        firmProposalRequestList.add(firmProposalRequest);
                    }
                }
            }
        }

        return firmProposalRequestList;
    }


     public FirmProposalRequest getFirmProposalsForCart(String externalId) {
        HttpHeaders headers = getAuthenticationHeaders(cpq_user, cpq_pass);

        HttpEntity<String> httpEntity = new HttpEntity<>("body", headers);

        RestTemplate template = new RestTemplate();
        String url = cpq_api_url + "/requestForFirmProposal/list?where=shoppingCart=" + externalId + CART_LIMIT;
        ResponseEntity<String> response = template.exchange(
                url,
                HttpMethod.GET,
                httpEntity,
                String.class,
                cartLimit);

        List<FirmProposalRequest> firmRequests = parseFirmProposalRequestAttributesFromResponse(response);

        return firmRequests.isEmpty() ? null : firmRequests.get(0);
        }


    private List<FirmProposalRequest> parseFirmProposalRequestAttributesFromResponse(ResponseEntity<String> response) {
        return parseFromResponse(response,
                resource -> {

                    FirmProposalRequest request = new FirmProposalRequest();
                    request.setState(resource.attr("stateId"));
                    request.setStateTitle(resource.attr("state"));
                    request.setID(resource.attr("id"));
                    Date d = parseDate(resource.attr("modifiedTime"));
                    request.setModifiedTime(d);
                    return request;
                },
                propReq -> {}
        );
    }


    public void deleteCart(String cpqCartId) {
        HttpHeaders headers = getAuthenticationHeaders(cpq_user, cpq_pass);

        HttpEntity<String> httpEntity = new HttpEntity<>("body", headers);
        RestTemplate template = new RestTemplate();
        template.exchange(
                cpq_api_url + "/shoppingcart/" + cpqCartId,
                HttpMethod.DELETE,
                httpEntity,
                String.class);
    }


    public List<CartAttributes> filterCartsCreated(List<CartAttributes> allCarts){
        //Filtering out deleted and firm quote carts
        List<CartAttributes> cartsFiltered = new ArrayList<>();
        for (CartAttributes cart : allCarts) {
            if ("Created".equalsIgnoreCase(cart.getState())) {
                cartsFiltered.add(cart);
            }
        }
        return cartsFiltered;
    }



    public void createNewCartAttributesForUser(User user) {
        Account account = accountService.getAccount(user.getAccount());
        if(account == null) {
            throw new CartCreateException("wrongUserAccountMessage");
        }

        CartAttributes attributes1 = new CartAttributes();
        String currentDateInMillis = String.valueOf(System.currentTimeMillis());
        attributes1.setExternalId(user.getId() + "_" + currentDateInMillis);
        attributes1.setReferenceExternalUser(user.getId().toString());
        attributes1.setName("Shopping Cart - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        attributes1.setCustomName("Shopping Cart - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        attributes1.setAccount(user.getAccount());
        attributes1.setCurrency(user.getCurrency());
        attributes1.setInstallationCountry(user.getCountryOfInstallation());
        setCartAttributes(user.getId() + "_" + currentDateInMillis, attributes1);

        //refresh currency after creating new cart
        List<CartAttributes> allCarts = findUserCarts(user.getId().toString());
        List<CartAttributes> carts = filterCartsCreated(allCarts);
        CartAttributes latestCartAttributes = getLatestCreatedCartAttributes(carts);
        refreshCurrency(latestCartAttributes);

    }


    public CartAttributes getLatestCreatedCartAttributes(List<CartAttributes> carts){
        CartAttributes latestCartAttributes = carts.stream()
                .sorted((o1, o2) -> o1.getLastModified().compareTo(o2.getLastModified()))
                .collect(Collectors.toList()).get(carts.size()-1);
        return latestCartAttributes;
    }



    public CartAttributes refreshCurrency(CartAttributes cartAttributes){

        RestTemplate restTemplate = new RestTemplate();
        String url = api_url + "/cart/refresh-currency?_externalId=" + cartAttributes.getExternalId();
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("X-Key", api_key);
        HttpEntity<String> request = new HttpEntity<String>(requestHeaders);

        ResponseEntity<CartAttributes> response = restTemplate.exchange(url, HttpMethod.POST, request, CartAttributes.class);
        CartAttributes cart = response.getBody();

        return cart;

    }

}
