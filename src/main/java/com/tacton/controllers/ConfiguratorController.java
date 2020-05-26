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
import com.tacton.services.NeedsParam;
import com.tacton.services.cpq.CartService;
import com.tacton.services.cpq.CountryService;
import com.tacton.services.cpq.CurrencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;


@Controller
public class ConfiguratorController {

    @Autowired
    private CartService cartService;

    @Autowired
    private CountryService countryService;

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private NeedsParam needsParam;

    @Value("${customer_self_service_api_key}")
    private String customer_self_service_api_key;

    @Value("${customer_self_service_api_url}")
    private String customer_self_service_api_url;

    @Value("${products_images_from_outside}")
    private String products_images_from_outside;

    @Value("${cpq_instance_url}")
    private String cpq_instance_url;

    @Value("${product_name}")
    private String product_name;

    @Value("${product_id}")
    private String product_id;

    @Value("${is_visualization_supported}")
    private String is_visualization_supported;

    @Value("${visualization_image}")
    private String visualization_image;

    @Value("${visualization_group_name}")
    private String visualization_group_name;

    @Value("${summary_group_name}")
    private String summary_group_name;

    @Value("${css_bom_columns}")
    private String css_bom_columns;

    @Value("${css_bom_extra_info}")
    private String css_bom_extra_info;

    @Value("${css_bom_show_prices}")
    private String css_bom_show_prices;


    @Value("${leadgen_api_key}")
    private String leadgen_api_key;

    @Value("${leadgen_config_url}")
    private String leadgen_config_url;

    @Value("${leadgen_bom_columns}")
    private String leadgen_bom_columns;

    @Value("${leadgen_bom_extra_info}")
    private String leadgen_bom_extra_info;

    @Value("${leadgen_bom_show_prices}")
    private String leadgen_bom_show_prices;

    @Value("${css_needs_params}")
    private String css_needs_params;

    @Value("${leadgen_needs_params}")
    private String leadgen_needs_params;

    private final Logger LOGGER = LoggerFactory.getLogger(ConfiguratorController.class);

    //
    //TODO
    //We should get product_name, is_visualization_supported, visualization_image ... from CPQ object rather than from config file.
    //This solution works for one configurable product.
    //
    @RequestMapping("/configure/{catalog}/{product}")
    public String configureProduct(@PathVariable String product, @PathVariable String catalog, Model model, HttpServletRequest request) throws Exception {

        String referer = request.getHeader("Referer");

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String cart = user.getActiveCartId();

        model.addAttribute("qty", 1);
        model.addAttribute("customer_self_service_api_key", customer_self_service_api_key);
        model.addAttribute("customer_self_service_api_url", customer_self_service_api_url);
        model.addAttribute("products_images_from_outside", products_images_from_outside);
        model.addAttribute("cpq_instance_url", cpq_instance_url);
        model.addAttribute("product_name", product_name);
        model.addAttribute("product_id", product);
        model.addAttribute("catalogTab", catalog);
        model.addAttribute("activeCartId", cart);
        model.addAttribute("referer", referer);
        model.addAttribute("is_visualization_supported", is_visualization_supported);
        model.addAttribute("visualization_image", visualization_image);
        model.addAttribute("visualization_group_name", visualization_group_name);
        model.addAttribute("summary_group_name", summary_group_name);
        model.addAttribute("bom_columns", css_bom_columns);
        model.addAttribute("bom_extra_info", css_bom_extra_info);
        model.addAttribute("bom_show_prices", css_bom_show_prices);
        model.addAttribute("needs_params", needsParam.prepareCartNeedsParams(css_needs_params, cart));
        model.addAttribute("cpq_instance_url", cpq_instance_url);

        return "configurator";
    }



    @RequestMapping("/reconfigure/{configId}")
    public String reconfigureProduct(@PathVariable String configId, Model model, HttpServletRequest request) throws Exception {

        String referer = request.getHeader("Referer");

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String cart = user.getActiveCartId();

        RestTemplate restTemplate = new RestTemplate();
        ShoppingCartItemList shoppingCartItemList = restTemplate.getForObject(customer_self_service_api_url + "/cart/items?_externalId=" + user.getActiveCartId() + "&_key=" + customer_self_service_api_key, ShoppingCartItemList.class);

        LOGGER.debug("cart items count=" + shoppingCartItemList.getItems().size());

        ShoppingCartItem shoppingCartItem = shoppingCartItemList.getItems().stream()
                .filter(item -> configId.equals(item.getId()))
                .findAny()
                .orElse(null);

        model.addAttribute("qty", shoppingCartItem.getQty());
        model.addAttribute("customer_self_service_api_key", customer_self_service_api_key);
        model.addAttribute("customer_self_service_api_url", customer_self_service_api_url);
        model.addAttribute("products_images_from_outside", products_images_from_outside);
        model.addAttribute("cpq_instance_url", cpq_instance_url);
        model.addAttribute("product_name", product_name);
        model.addAttribute("product_id", configId);
        model.addAttribute("catalogTab", "");
        model.addAttribute("activeCartId", cart);
        model.addAttribute("referer", referer);
        model.addAttribute("is_visualization_supported", is_visualization_supported);
        model.addAttribute("visualization_image", visualization_image);
        model.addAttribute("visualization_group_name", visualization_group_name);
        model.addAttribute("summary_group_name", summary_group_name);
        model.addAttribute("bom_columns", css_bom_columns);
        model.addAttribute("bom_extra_info", css_bom_extra_info);
        model.addAttribute("bom_show_prices", css_bom_show_prices);
        model.addAttribute("needs_params", needsParam.prepareCartNeedsParams(css_needs_params, cart));
        model.addAttribute("cpq_instance_url", cpq_instance_url);

        return "configurator";
    }


    //Leadgen
    @RequestMapping(path = "/configure")
    public String leadgenConfigure(Model model) throws Exception {

        model.addAttribute("api_key", leadgen_api_key);
        model.addAttribute("config_url", leadgen_config_url);
        model.addAttribute("products_images_from_outside", products_images_from_outside);
        model.addAttribute("product_name", product_name);
        model.addAttribute("product_id", product_id);
        model.addAttribute("is_visualization_supported", is_visualization_supported);
        model.addAttribute("visualization_image", visualization_image);
        model.addAttribute("visualization_group_name", visualization_group_name);
        model.addAttribute("summary_group_name", summary_group_name);
        model.addAttribute("bom_columns", leadgen_bom_columns);
        model.addAttribute("bom_extra_info", leadgen_bom_extra_info);
        model.addAttribute("bom_show_prices", leadgen_bom_show_prices);
        model.addAttribute("needs_params", needsParam.prepareLeadGenNeedsParams(leadgen_needs_params, () -> ""));
        model.addAttribute("cpq_instance_url", cpq_instance_url);

        return "configuratorLeadgen";
    }

    @RequestMapping(path = "/configure-needs/{param}")
    public String leadgenConfigure(@PathVariable String param, Model model) throws Exception {

        model.addAttribute("api_key", leadgen_api_key);
        model.addAttribute("config_url", leadgen_config_url);
        model.addAttribute("products_images_from_outside", products_images_from_outside);
        model.addAttribute("product_name", product_name);
        model.addAttribute("product_id", product_id);
        model.addAttribute("is_visualization_supported", is_visualization_supported);
        model.addAttribute("visualization_image", visualization_image);
        model.addAttribute("visualization_group_name", visualization_group_name);
        model.addAttribute("summary_group_name", summary_group_name);
        model.addAttribute("bom_columns", leadgen_bom_columns);
        model.addAttribute("bom_extra_info", leadgen_bom_extra_info);
        model.addAttribute("bom_show_prices", leadgen_bom_show_prices);
        model.addAttribute("needs_params", needsParam.prepareLeadGenNeedsParams(leadgen_needs_params, () -> param));
        model.addAttribute("cpq_instance_url", cpq_instance_url);

        return "configuratorLeadgen";
    }


}
