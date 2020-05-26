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

import com.tacton.entities.User;
import com.tacton.entities.cpqresponse.*;
import com.tacton.services.cpq.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Controller
public class ShopController {


    private final Logger LOGGER = LoggerFactory.getLogger(ShopController.class);

    @Value("${customer_self_service_api_key}")
    private String customer_self_service_api_key;

    @Value("${customer_self_service_api_url}")
    private String customer_self_service_api_url;

    @Value("${cpq_instance_url}")
    private String cpq_instance_url;

    @Value("${shop_initial_tab}")
    private String shop_initial_tab;

    private static String CATALOG_TABS_URI = "/catalog/tabs";

    private static String API_KEY_PARAM = "?_key={api_key}";

    private static String LIMIT_PARAM = "&limit={limit}";

    @Value("${catalog_limit}")
    private String catalogLimit;

    @Autowired
    CartService cartService;


    @RequestMapping("/shop")
    public String getShopHomepage(Model model) throws Exception{

        RestTemplate restTemplate = new RestTemplate();

        Catalog[] catalogues = restTemplate.getForObject(customer_self_service_api_url + CATALOG_TABS_URI + API_KEY_PARAM, Catalog[].class, customer_self_service_api_key);
        List<Catalog> catalogTabsList = Arrays.asList(catalogues);
        model.addAttribute("catalogTabsList", catalogTabsList);

        //Display first from the list from CPQ
        Catalog currentCatalog = catalogTabsList.stream()
                .filter(cat -> shop_initial_tab.equals(cat.getType()))
                .findAny()
                .orElseGet(() -> catalogTabsList.get(0));
        model.addAttribute("currentCatalog", currentCatalog);

        ProductList productList = restTemplate.getForObject(customer_self_service_api_url + "/catalog/search/" + currentCatalog.getName()  + API_KEY_PARAM + LIMIT_PARAM,
                ProductList.class, customer_self_service_api_key, catalogLimit);

        for (Product p : productList.getProducts()){
            if(p.getImage()!=null && !p.getImage().isEmpty()) {
                //Call to get image file
                String url = cpq_instance_url + p.getImage();
                HttpHeaders requestHeaders = new HttpHeaders();
                requestHeaders.add("X-Key", customer_self_service_api_key);
                HttpEntity<String> request = new HttpEntity<String>(requestHeaders);
                ResponseEntity<Resource> response = restTemplate.exchange(url, HttpMethod.GET, request, Resource.class);

                //Saving file from response
                InputStream stream = response.getBody().getInputStream();
                byte[] buffer = stream.readAllBytes();
                stream.close();

                //encoding and setting image as a member variable of product
                p.setImg(Base64.getEncoder().encodeToString(buffer));
            }
        }

        model.addAttribute("products", productList.getProducts());

        return "shop";

    }



    @RequestMapping("/shop/{name}")
    public String getSpecificCatalog(@PathVariable String name, Model model) throws Exception{

        RestTemplate restTemplate = new RestTemplate();

        Catalog[] catalogues = restTemplate.getForObject(customer_self_service_api_url + CATALOG_TABS_URI + API_KEY_PARAM, Catalog[].class, customer_self_service_api_key);
        List<Catalog> catalogTabsList = Arrays.asList(catalogues);
        model.addAttribute("catalogTabsList", catalogTabsList);

        Catalog currentCatalog = null;
        for(Catalog c : catalogTabsList){
            if(c.getName().equals(name)){
                currentCatalog = c;
            }
        }
        model.addAttribute("currentCatalog", currentCatalog);

        ProductList productList = restTemplate.getForObject(customer_self_service_api_url + "/catalog/search/" + name  + API_KEY_PARAM + LIMIT_PARAM,
                ProductList.class, customer_self_service_api_key, catalogLimit);

        for (Product p : productList.getProducts()){
            if(p.getImage()!=null && !p.getImage().isEmpty()) {
                //Call to get image file
                String url = cpq_instance_url + p.getImage();
                HttpHeaders requestHeaders = new HttpHeaders();
                requestHeaders.add("X-Key", customer_self_service_api_key);
                HttpEntity<String> request = new HttpEntity<String>(requestHeaders);
                ResponseEntity<Resource> response = restTemplate.exchange(url, HttpMethod.GET, request, Resource.class);

                //Saving file from response
                InputStream stream = response.getBody().getInputStream();
                byte[] buffer = stream.readAllBytes();
                stream.close();

                //encoding and setting image as a member variable of product
                p.setImg(Base64.getEncoder().encodeToString(buffer));
            }
        }

        model.addAttribute("products", productList.getProducts());

        return "shop";
    }



    @RequestMapping("/add/{catalog}/{product}/{count}")
    public String addNonConfigurable(@PathVariable String catalog, @PathVariable String product, @PathVariable String count, @RequestHeader(value = "referer", required = false) String referer, Model model, HttpSession session) {

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        RestTemplate restTemplate = new RestTemplate();
        ShoppingCartItemList shoppingCartItemList = restTemplate.getForObject(customer_self_service_api_url + "/cart/items?_externalId=" + user.getActiveCartId() + "&_key=" + customer_self_service_api_key, ShoppingCartItemList.class);


        ShoppingCartItem shoppingCartItem = shoppingCartItemList.getItems().stream()
                .filter(item -> product.equals(item.getProductId()))
                .findAny()
                .orElse(null);

        LOGGER.debug("adding qty=" + count);

        if(shoppingCartItem!=null){
            RestTemplate restTemplate2 = new RestTemplate();
            String url = customer_self_service_api_url + "/cart/items/" + shoppingCartItem.getId() + "?_externalId=" + user.getActiveCartId() + "&qty=" + (shoppingCartItem.getQty() + Integer.parseInt(count));
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.add("X-Key", customer_self_service_api_key);
            HttpEntity<String> request = new HttpEntity<String>(requestHeaders);

            LOGGER.debug("add to existing cart url=" + url);

            restTemplate2.exchange(url, HttpMethod.PUT, request, Resource.class);
        }
        else{
            RestTemplate restTemplate2 = new RestTemplate();
            String url = customer_self_service_api_url + "/cart/items?_externalId=" + user.getActiveCartId() + "&tab=" + catalog + "&productId=" + product + "&qty=" + Integer.parseInt(count);
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.add("X-Key", customer_self_service_api_key);
            HttpEntity<String> request = new HttpEntity<String>(requestHeaders);

            LOGGER.debug("create new cart url=" + url);

            restTemplate2.exchange(url, HttpMethod.POST, request, Resource.class);
        }


        user.setActiveCart(cartService.getCartAttributes(user.getActiveCartId()));

        Authentication auth = new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        return "redirect:"+referer;
    }


}
