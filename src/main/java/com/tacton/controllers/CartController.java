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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tacton.entities.User;
import com.tacton.entities.cpqresponse.*;
import com.tacton.services.cpq.*;
import com.tacton.services.UserService;
import com.tacton.services.cpq.CartService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.*;
import java.io.IOException;
import java.util.function.Function;

@Controller
public class CartController {

    @Autowired
    private UserService userService;

    @Autowired
    private CartService cartService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private CountryService countryService;

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private MessageSource messageSource;

    @Value("${customer_self_service_api_key}")
    private String customer_self_service_api_key;

    @Value("${customer_self_service_api_url}")
    private String customer_self_service_api_url;

    @Value("${cpq_api_url}")
    private String cpq_api_url;

    @Value("${cpq_user}")
    private String cpq_user;

    @Value("${cpq_pass}")
    private String cpq_pass;

    @Value("${cart_columns}")
    private String cart_columns;

    @Value("${cart_totals}")
    private String cart_totals;

    @Value("${cart_percent_columns}")
    private String cart_percent_columns;

    @Value("${cart_currency_columns}")
    private String cart_currency_columns;

    @Value("${cart_list_columns}")
    private String cart_list_columns;

    @Value("${cart_list_percent_columns}")
    private String cart_list_percent_columns;

    @Value("${cart_list_currency_columns}")
    private String cart_list_currency_columns;

    @Value("${firm_quote_request_columns}")
    private String firm_quote_request_columns;

    @Value("${firm_quote_request_percent_columns}")
    private String firm_quote_request_percent_columns;

    @Value("${firm_quote_request_currency_columns}")
    private String firm_quote_request_currency_columns;

    private static String CART_ATTRIBUTES_URI = "/cart";

    private static String API_KEY_PARAM = "?_key={api_key}";

    private static String EXTERNAL_ID_PARAM = "&_externalId={external_id}";

    private static TableDefinition cartTableDefinition;
    private static TableDefinition cartListTableDefinition;
    private static TableDefinition firmQuoteRequestsTableDefinition;

    private final Logger LOGGER = LoggerFactory.getLogger(CartController.class);

    @RequestMapping(path = "/myCartList", method = RequestMethod.GET)
    public String userCarts(Model model) {
        RestTemplate restTemplate = new RestTemplate();
        User userFromAuthentication = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userService.findById(userFromAuthentication.getId());

        List<CartAttributes> allCarts = cartService.findUserCarts(user.getId().toString());

        List<CartAttributes> filteredCarts = new ArrayList<>();
        for (CartAttributes cart : allCarts) {
            if ("Created".equalsIgnoreCase(cart.getState())) {
                ShoppingCartItemList shoppingCartItemList = restTemplate.getForObject(
                        customer_self_service_api_url + "/cart/items?_externalId=" + cart.getExternalId() + "&_key=" + customer_self_service_api_key, ShoppingCartItemList.class);
                cart.setCnt(shoppingCartItemList.getItems().size());
                filteredCarts.add(cart);
            }
        }
        Map<String, String> currencyMap = new HashMap<>();

        model.addAttribute("carts", filteredCarts);
        model.addAttribute("tableDefinition", getCartListTableDefinition(currencyProvider(currencyMap)));

        return "myCartList";
    }

    private TableDefinition getCartListTableDefinition(Function<CpqEntity, String> currencyProvider) {
        if(cartListTableDefinition == null) {
            cartListTableDefinition = new TableDefinition(
                    cart_list_columns.split(","),
                    currencyProvider,
                    cart_list_percent_columns.split(","),
                    cart_list_currency_columns.split(","));
        }
        return cartListTableDefinition;
    }

    @RequestMapping(path = "/myFirmQuoteRequests", method = RequestMethod.GET)
    public String myFirmQuoteRequests(Model model) {

        RestTemplate restTemplate = new RestTemplate();
        User userFromAuthentication = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userService.findById(userFromAuthentication.getId());

        List<CartAttributes> allCarts = cartService.findUserCarts(user.getId().toString());
        List<FirmProposalRequest> firmProposalRequests = cartService.getAllFirmProposalRequestsForCarts(allCarts);

        Map<String, String> currencyMap = new HashMap<>();

        model.addAttribute("firmRequests", firmProposalRequests);
        model.addAttribute("tableDefinition", getFirmQuoteRequestsTableDefinition(currencyProvider(currencyMap)));

        return "myFirmQuoteRequests";
    }

    private TableDefinition getFirmQuoteRequestsTableDefinition(Function<CpqEntity, String> currencyProvider) {
        if(firmQuoteRequestsTableDefinition == null) {
            firmQuoteRequestsTableDefinition = new TableDefinition(
                    firm_quote_request_columns.split(","),
                    currencyProvider,
                    firm_quote_request_percent_columns.split(","),
                    firm_quote_request_currency_columns.split(","));
        }
        return firmQuoteRequestsTableDefinition;
    }

    @RequestMapping(path = "/myFirmRequests", method = RequestMethod.GET)
    public String userFirmRequest(Model model) {
        return "redirect:/myCartList";
    }

    @RequestMapping(path = "/cart/{id}/proposal")
    public @ResponseBody byte[] getDraftProposal(@PathVariable String id) throws IOException {
        return cartService.getDraftProposal(id);
    }

    @RequestMapping(path = "/cart/{id}/requestfirmproposal")
    public String requestFirmProposal(@PathVariable String id, RedirectAttributes redirectAttrs) throws IOException {

        RequestFirmProposalResponse response;
        try {
            response = cartService.requestFirmProposal(id);
        } catch(HttpClientErrorException ex) {
            return redirectWithMessage(redirectAttrs, "/myCart",
                    "warning", "cannotRequestQuoteEmptyCart");
        }
        redirectAttrs.addFlashAttribute("requestFirmProposalResponse", response);

        //changing active cart for dropdown (we don't want to display cart with status "Firm proposal requested" in dropdown)
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        try {
            userService.setActiveCartOrCreateOne(user);
        } catch(HttpClientErrorException ex) {
            throw new CartCreateException("networkProblem");
        } finally {
            Authentication auth = new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        return redirectWithMessage(redirectAttrs, "/myFirmQuoteRequests",
                "done", "requestConfirmation");
    }



    @ResponseBody
    @RequestMapping(path = "/changeActiveCart/{id}", method = RequestMethod.GET)
    public String changeActiveCart(@PathVariable String id) {

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        user.setActiveCartId(id);
        userService.save(user);

        user.setActiveCart(cartService.getCartAttributes(user.getActiveCartId()));

        Authentication auth = new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        return "ok";
    }


    @RequestMapping(path = "cart/{id}/edit", method = RequestMethod.GET)
    public String editCart(@PathVariable String id) {

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        user.setActiveCartId(id);
        userService.save(user);

        user.setActiveCart(cartService.getCartAttributes(user.getActiveCartId()));

        Authentication auth = new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        return "redirect:/myCart";
    }

    private String redirectWithMessage(RedirectAttributes redirectAttributes, String redirect, String headerKey, String messageKey) {
        redirectAttributes.addFlashAttribute("header", messageSource.getMessage(headerKey, null, Locale.getDefault()));
        redirectAttributes.addFlashAttribute("message", messageSource.getMessage(messageKey, null, Locale.getDefault()) );
        return "redirect:"+redirect;
    }

    @RequestMapping(path = "/myCart", method = RequestMethod.GET)
    public String myCart(Model model, RedirectAttributes redirectAttributes) throws IOException {
        User userFromAuthentication = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userService.findById(userFromAuthentication.getId());

        CartAttributes shoppingCart = cartService.getCartFromCPQapi(user.getActiveCartId());
        if(shoppingCart == null) {
            return redirectWithMessage(redirectAttributes,"/myCartList",  "warning", "wrongCartMessage");
        }

        Account account = accountService.getAccount(shoppingCart.getAccount());
        if( account == null) {
            return redirectWithMessage(redirectAttributes,"/myCartList",  "warning", "wrongCartAccountMessage");
        }

        RestTemplate restTemplate = new RestTemplate();
        ShoppingCartItemList shoppingCartItemList = restTemplate.getForObject(customer_self_service_api_url + "/cart/items?_externalId=" + user.getActiveCartId() + "&_key=" + customer_self_service_api_key, ShoppingCartItemList.class);

        for(ShoppingCartItem item : shoppingCartItemList.getItems()){
            if(item.getCatalogImage()!=null && !item.getCatalogImage().isEmpty()) {
                //Call to get image file
                HttpHeaders headers = new HttpHeaders();
                String plainCreds = cpq_user + ":" + cpq_pass;
                byte[] plainCredsBytes = plainCreds.getBytes();
                byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
                String base64Creds = new String(base64CredsBytes);
                headers.add("Authorization", "Basic " + base64Creds);
                // headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
                HttpEntity<String> entity = new HttpEntity<String>(headers);

                String url = cpq_api_url + "/shoppingcartitem/" + item.getId() + "/download/catalogImage";
                HttpEntity<String> request = new HttpEntity<String>(headers);
                ResponseEntity<Resource> response = restTemplate.exchange(url, HttpMethod.GET, request, Resource.class);

                //Saving file from response
                InputStream stream = response.getBody().getInputStream();
                byte[] buffer = stream.readAllBytes();
                stream.close();

                //encoding and setting image as a member variable of product
                item.setImage(Base64.encodeBase64String(buffer));
            }

            //call to find if DA/CAD supported
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-key", customer_self_service_api_key);
            HttpEntity<String> entity = new HttpEntity<String>(headers);

            String url = customer_self_service_api_url + "/cart/items/" + item.getId() + "/cad/cad-automation-status";
            HttpEntity<String> request = new HttpEntity<String>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            JsonNode parent= new ObjectMapper().readTree(response.getBody());
            String cadSupport = parent.path("status").asText();

            if (cadSupport.equals("pending") || cadSupport.equals("not generated")) {
                item.setCadSupport(true);
            } else if (cadSupport.equals("finished")) {
                item.setCadSupport(true);

                //call to get list of files
                String urlListFiles = customer_self_service_api_url + "/cart/items/" + item.getId() + "/cad/get-generated-cad-document-list";
                HttpEntity<String> requestListfiles = new HttpEntity<String>(headers);
                ResponseEntity<String> responseListFiles = restTemplate.exchange(urlListFiles, HttpMethod.GET, requestListfiles, String.class);
                JsonNode parentNode = new ObjectMapper().readTree(responseListFiles.getBody());
                String files = String.valueOf(parentNode.path("files"));
                final ObjectMapper objectMapper = new ObjectMapper();
                CadDocument[] documents = objectMapper.readValue(files, CadDocument[].class);

                for (CadDocument cadDocument : documents) {
                    String fileUrl = cadDocument.getFile();
                    String fileName = fileUrl.substring( fileUrl.lastIndexOf('/')+1, fileUrl.length() );
                    String fileNameWithoutExtn = fileName.substring(0, fileName.lastIndexOf('.'));
                    fileNameWithoutExtn = fileNameWithoutExtn.replaceAll("%20", " ");
                    cadDocument.setName(fileNameWithoutExtn);
                }
                item.setCadDocuments(documents);
            } else {
                item.setCadSupport(false);
            }
            item.setCadDocumentsStatus(cadSupport);
        }
        Map<String, String> currencyMap = new HashMap<>();

        model.addAttribute("account", account);
        model.addAttribute("country", countryService.getCountry(shoppingCart.getInstallationCountry()));
        model.addAttribute("currency", currencyService.getCurrency(shoppingCart.getCurrency()));
        model.addAttribute("shoppingCartItemList", shoppingCartItemList.getItems());
        model.addAttribute("shoppingCart", shoppingCart);
        model.addAttribute("tableDefinition", getCartTableDefinition(currencyProvider(currencyMap)));

        return "myCart";
    }

    private Function<CpqEntity, String> currencyProvider(Map<String, String> currencyMap)  {
        return entity -> {
            String currencyId = entity.getCurrency();

            return currencyId == null
                    ? null
                    : currencyMap.computeIfAbsent(currencyId, cur -> currencyService.getCurrency(cur).getIsoCode());
        };
    }

    private TableDefinition getCartTableDefinition(Function<CpqEntity, String> currencyProvider) {
        if(cartTableDefinition == null) {
            cartTableDefinition = new TableDefinition(
                    cart_columns.split(","),
                    cart_totals.split(","),
                    currencyProvider,
                    cart_percent_columns.split(","),
                    cart_currency_columns.split(","));
         }
        return cartTableDefinition;
    }


    @ResponseBody
    @RequestMapping(path = "/newCart", method = RequestMethod.GET)
    public CreateStatus newCart() {

        User userFromAuth = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userService.findById(userFromAuth.getId());

        try {
        cartService.createNewCartAttributesForUser(user);
        } catch (CartCreateException ex) {
            LOGGER.warn("Cannot create new cart", ex);
            String header = messageSource.getMessage("error", null, Locale.getDefault());
            String createMsg = "wrongUserAccountMessage".equals(ex.getMessage()) ? "cannotCreateNewCart" : ex.getMessage();
            String message = messageSource.getMessage(createMsg, null, Locale.getDefault());
            return new CreateStatus("error", header, message);
        }

        List<CartAttributes> allCarts = cartService.findUserCarts(user.getId().toString());

        //Filtering out deleted and firm quote carts
        List<CartAttributes> carts = cartService.filterCartsCreated(allCarts);

        CartAttributes latestCartAttributes = cartService.getLatestCreatedCartAttributes(carts);

        latestCartAttributes = cartService.refreshCurrency(latestCartAttributes);

        user.setActiveCartId(latestCartAttributes.getExternalId());
        user.setActiveCart(latestCartAttributes);
        user.setCarts(carts);
        userService.save(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        return new CreateStatus("ok", "Done", "");
    }



    @ResponseBody
    @RequestMapping(path = "/updateActiveCart", method = RequestMethod.GET)
    public String updateActiveCart() {

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        user.setActiveCart(cartService.getCartAttributes(user.getActiveCartId()));

        Authentication auth = new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        return "ok";
    }

    @RequestMapping("/removeCart/{shoppingCartCPQId}")
    public String removeShoppingCart(@PathVariable String shoppingCartCPQId, RedirectAttributes redirectAttrs) {

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Remove the ShoppingCart in CPQ
        cartService.deleteCart(shoppingCartCPQId);

        try {
            userService.setActiveCartOrCreateOne(user);
        } finally {
            LOGGER.info("active cart=" + user.getActiveCart().getName());

            Authentication auth = new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        return redirectWithMessage(redirectAttrs, "/myCartList", "done", "cartDeleted");
    }

    @RequestMapping("/remove/{shoppingCartItemId}")
    public String removeItemFromCart(@PathVariable String shoppingCartItemId) {

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String cartId = user.getActiveCartId();

        RestTemplate restTemplate = new RestTemplate();
        String url = customer_self_service_api_url + "/cart/items/" + shoppingCartItemId + "?_externalId=" + cartId;
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("X-Key", customer_self_service_api_key);
        HttpEntity<String> request = new HttpEntity<String>(requestHeaders);
        restTemplate.exchange(url, HttpMethod.DELETE, request, Resource.class);

        user.setActiveCart(cartService.getCartAttributes(user.getActiveCartId()));

        Authentication auth = new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        return "redirect:/myCart";
    }



    @RequestMapping("/addOne/{shoppingCartItemId}")
    public String addOneItem(@PathVariable String shoppingCartItemId) {

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String cartId = user.getActiveCartId();

        RestTemplate restTemplate = new RestTemplate();
        ShoppingCartItemList shoppingCartItemList = restTemplate.getForObject(customer_self_service_api_url + "/cart/items?_externalId=" + user.getActiveCartId() + "&_key=" + customer_self_service_api_key, ShoppingCartItemList.class);

        ShoppingCartItem shoppingCartItem = shoppingCartItemList.getItems().stream()
                .filter(item -> shoppingCartItemId.equals(item.getId()))
                .findAny()
                .orElse(null);

        if(shoppingCartItem !=null) {
            RestTemplate restTemplate2 = new RestTemplate();
            String url = customer_self_service_api_url + "/cart/items/" + shoppingCartItemId + "?_externalId=" + user.getActiveCartId() + "&qty=" + (shoppingCartItem.getQty() + 1);
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.add("X-Key", customer_self_service_api_key);
            HttpEntity<String> request = new HttpEntity<String>(requestHeaders);
            restTemplate2.exchange(url, HttpMethod.PUT, request, Resource.class);
        }

        RestTemplate restTemplate3 = new RestTemplate();
        String url3 = customer_self_service_api_url + "/cart/recalculate-prices?_externalId=" + user.getActiveCartId();
        HttpHeaders requestHeaders3 = new HttpHeaders();
        requestHeaders3.add("X-Key", customer_self_service_api_key);
        HttpEntity<String> request3 = new HttpEntity<String>(requestHeaders3);
        restTemplate3.exchange(url3, HttpMethod.POST, request3, Resource.class);



        user.setActiveCart(cartService.getCartAttributes(user.getActiveCartId()));

        Authentication auth = new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        return "redirect:/myCart";
    }



    @RequestMapping("/removeOne/{shoppingCartItemId}")
    public String removeOneItem(@PathVariable String shoppingCartItemId) {

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String cartId = user.getActiveCartId();

        RestTemplate restTemplate = new RestTemplate();
        ShoppingCartItemList shoppingCartItemList = restTemplate.getForObject(customer_self_service_api_url + "/cart/items?_externalId=" + user.getActiveCartId() + "&_key=" + customer_self_service_api_key, ShoppingCartItemList.class);

        ShoppingCartItem shoppingCartItem = shoppingCartItemList.getItems().stream()
                .filter(item -> shoppingCartItemId.equals(item.getId()))
                .findAny()
                .orElse(null);

        if(shoppingCartItem !=null) {
            RestTemplate restTemplate2 = new RestTemplate();
            String url = customer_self_service_api_url + "/cart/items/" + shoppingCartItemId + "?_externalId=" + user.getActiveCartId() + "&qty=" + (shoppingCartItem.getQty() - 1);
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.add("X-Key", customer_self_service_api_key);
            HttpEntity<String> request = new HttpEntity<String>(requestHeaders);
            restTemplate2.exchange(url, HttpMethod.PUT, request, Resource.class);
        }

        RestTemplate restTemplate3 = new RestTemplate();
        String url3 = customer_self_service_api_url + "/cart/recalculate-prices?_externalId=" + user.getActiveCartId();
        HttpHeaders requestHeaders3 = new HttpHeaders();
        requestHeaders3.add("X-Key", customer_self_service_api_key);
        HttpEntity<String> request3 = new HttpEntity<String>(requestHeaders3);
        restTemplate3.exchange(url3, HttpMethod.POST, request3, Resource.class);



        user.setActiveCart(cartService.getCartAttributes(user.getActiveCartId()));

        Authentication auth = new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        return "redirect:/myCart";
    }


    @ResponseBody
    @RequestMapping(path = "/changeCartName", method = RequestMethod.POST)
    public String changeCartCustomName(@RequestParam("externalId") String externalId, @RequestParam("newName") String newName){

        if(StringUtils.isEmpty(newName)){
            return "error";
        }

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String externalIdFromSession = user.getActiveCartId();

        CartAttributes cartAttributes = cartService.getCartAttributes(externalIdFromSession);
        cartAttributes.setCustomName(newName);
        cartService.setCartAttributes(externalIdFromSession, cartAttributes);

       // userService.setActiveCartOrCreateOne(user);

        List<CartAttributes> allCarts = cartService.findUserCarts(user.getId().toString());
        //Filtering out deleted and firm quote carts
        List<CartAttributes> carts = cartService.filterCartsCreated(allCarts);
        user.setCarts(carts);


        Authentication auth = new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        return "ok";
    }

    @ResponseBody
    @RequestMapping("/cad/generate/{shoppingCartItemId}")
    public ResponseEntity<String> generateCadDocuments(@PathVariable String shoppingCartItemId) throws Exception {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        RestTemplate restTemplate = new RestTemplate();
        ShoppingCartItemList shoppingCartItemList = restTemplate.getForObject(customer_self_service_api_url + "/cart/items?_externalId=" + user.getActiveCartId() + "&_key=" + customer_self_service_api_key, ShoppingCartItemList.class);

        ShoppingCartItem shoppingCartItem = shoppingCartItemList.getItems().stream()
                .filter(item -> shoppingCartItemId.equals(item.getId()))
                .findAny()
                .orElse(null);

        if(shoppingCartItem !=null) {
            RestTemplate restTemplate2 = new RestTemplate();
            String url = customer_self_service_api_url + "/cart/items/" + shoppingCartItem.getId() + "/cad/start-cad-automation";
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.add("X-Key", customer_self_service_api_key);
            HttpEntity<String> request = new HttpEntity<String>(requestHeaders);
            ResponseEntity<String> response = restTemplate2.exchange(url, HttpMethod.POST, request, String.class);
            return ResponseEntity.ok()
                    .body("ok");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping("/cad/download/{shoppingCartItemId}")
    public ResponseEntity<Resource> downloadCadDocuments(@PathVariable String shoppingCartItemId) throws Exception {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        RestTemplate restTemplate = new RestTemplate();
        ShoppingCartItemList shoppingCartItemList = restTemplate.getForObject(customer_self_service_api_url + "/cart/items?_externalId=" + user.getActiveCartId() + "&_key=" + customer_self_service_api_key, ShoppingCartItemList.class);

        ShoppingCartItem shoppingCartItem = shoppingCartItemList.getItems().stream()
                .filter(item -> shoppingCartItemId.equals(item.getId()))
                .findAny()
                .orElse(null);

        if(shoppingCartItem !=null) {
            RestTemplate restTemplate2 = new RestTemplate();
            String url = customer_self_service_api_url + "/cart/items/" + shoppingCartItem.getId() + "/cad/download-all-generated-cad-documents";
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.add("X-Key", customer_self_service_api_key);
            HttpEntity<String> request = new HttpEntity<String>(requestHeaders);
            ResponseEntity<Resource> response = restTemplate2.exchange(url, HttpMethod.GET, request, Resource.class);

            //Saving file from response
            InputStream stream = response.getBody().getInputStream();
            byte[] buffer = stream.readAllBytes();
            stream.close();
            ByteArrayResource resource = new ByteArrayResource(buffer);

            HttpHeaders header = new HttpHeaders();
            header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + shoppingCartItem.getProductCatalogId() + " CAD Documents.zip");
            header.add("Cache-Control", "no-cache, no-store, must-revalidate");
            header.add("Pragma", "no-cache");
            header.add("Expires", "0");

            return ResponseEntity.ok()
                    .headers(header)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @ResponseBody
    @RequestMapping(path = "/cad/status/{shoppingCartItemId}", method = RequestMethod.GET)
    public ResponseEntity<String> checkCadDocumentsStatus(@PathVariable String shoppingCartItemId) throws Exception {

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        RestTemplate restTemplate = new RestTemplate();
        ShoppingCartItemList shoppingCartItemList = restTemplate.getForObject(customer_self_service_api_url + "/cart/items?_externalId=" + user.getActiveCartId() + "&_key=" + customer_self_service_api_key, ShoppingCartItemList.class);

        ShoppingCartItem shoppingCartItem = shoppingCartItemList.getItems().stream()
                .filter(item -> shoppingCartItemId.equals(item.getId()))
                .findAny()
                .orElse(null);

        if(shoppingCartItem !=null) {
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-key", customer_self_service_api_key);
            HttpEntity<String> entity = new HttpEntity<String>(headers);
            String url = customer_self_service_api_url + "/cart/items/" + shoppingCartItem.getId() + "/cad/cad-automation-status";
            HttpEntity<String> request = new HttpEntity<String>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            JsonNode parent = new ObjectMapper().readTree(response.getBody());
            String status = parent.path("status").asText();
            return ResponseEntity.ok().body(status);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @ResponseBody
    @RequestMapping(path = "/cad/list/{shoppingCartItemId}", method = RequestMethod.GET)
    public ResponseEntity<CadDocument[]> getCadDocumentList(@PathVariable String shoppingCartItemId) throws Exception {

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        RestTemplate restTemplate = new RestTemplate();
        ShoppingCartItemList shoppingCartItemList = restTemplate.getForObject(customer_self_service_api_url + "/cart/items?_externalId=" + user.getActiveCartId() + "&_key=" + customer_self_service_api_key, ShoppingCartItemList.class);

        ShoppingCartItem shoppingCartItem = shoppingCartItemList.getItems().stream()
                .filter(item -> shoppingCartItemId.equals(item.getId()))
                .findAny()
                .orElse(null);

        if(shoppingCartItem !=null) {
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-key", customer_self_service_api_key);
            HttpEntity<String> entity = new HttpEntity<String>(headers);
            String url = customer_self_service_api_url + "/cart/items/" + shoppingCartItem.getId() + "/cad/get-generated-cad-document-list";
            HttpEntity<String> request = new HttpEntity<String>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            JsonNode parent = new ObjectMapper().readTree(response.getBody());
            String files = String.valueOf(parent.path("files"));
            final ObjectMapper objectMapper = new ObjectMapper();
            CadDocument[] documents = objectMapper.readValue(files, CadDocument[].class);

            for (CadDocument cadDocument : documents) {
                String fileUrl = cadDocument.getFile();
                String fileName = fileUrl.substring( fileUrl.lastIndexOf('/')+1, fileUrl.length() );
                String fileNameWithoutExtn = fileName.substring(0, fileName.lastIndexOf('.'));
                fileNameWithoutExtn = fileNameWithoutExtn.replaceAll("%20", " ");
                cadDocument.setName(fileNameWithoutExtn);
            }

            return ResponseEntity.ok().body(documents);
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    @ExceptionHandler(CartCreateException.class)
    public ModelAndView handleCartCreateException(HttpServletRequest request, Exception ex, RedirectAttributes redirectAttributes){
        request.getSession().invalidate();

        String message = messageSource.getMessage(ex.getMessage(), null, Locale.getDefault());

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("header", "Error");
        modelAndView.addObject("message", message);

        modelAndView.setViewName("notificationPage");

        return modelAndView;
    }


    public static class CreateStatus {
        public final String status;
        public final String header;
        public final String message;

        public CreateStatus(String s, String h, String m) {
            this.status = s;
            this.header = h;
            this.message = m;
        }
    }
}
