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
import com.tacton.services.UserService;
import com.tacton.services.cpq.AccountService;
import com.tacton.services.cpq.CartService;
import com.tacton.services.cpq.CountryService;
import com.tacton.services.cpq.CurrencyService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


@Controller
public class AdminController {


    @Autowired
    UserService userService;

    @Autowired
    CartService cartService;

    @Autowired
    CountryService countryService;

    @Autowired
    CurrencyService currencyService;

    @Autowired
    AccountService accountService;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    BCryptPasswordEncoder encoder;

    private final Logger LOGGER = LoggerFactory.getLogger(AdminController.class);

    @RequestMapping(path = "/admin")
    public String getAdminDashboard(Model model){

        List<User> userList = userService.findAll();
        userList = userList.stream().limit(20).collect(Collectors.toList());
        model.addAttribute("userList", userList);

        List<CartAttributes> cartsAll = cartService.findAllCarts();

        model.addAttribute("cartsCount", cartsAll.size());
        cartsAll = cartsAll.stream().limit(20).collect(Collectors.toList());
        model.addAttribute("cartsList", cartsAll);


        List<FirmProposalRequest> proposalRequestList = cartService.getAllFirmProposalRequestsForCarts(cartsAll);
        model.addAttribute("proposalRequestCount", proposalRequestList.size());
        proposalRequestList = proposalRequestList.stream().limit(20).collect(Collectors.toList());
        model.addAttribute("proposalRequestList", proposalRequestList);

        return "admin/dashboard";
    }



    @RequestMapping(path = "/admin/users")
    public String getAdminUsers(Model model){

        List<User> userList = userService.findAll();
        model.addAttribute("userList", userList);


        return "admin/users";
    }


    @RequestMapping(path = "/admin/user/{id}", method = RequestMethod.GET)
    public String getAdminUser(@PathVariable String id, Model model){

        User user = userService.findById(Long.parseLong(id));
        model.addAttribute("user", user);

        LOGGER.debug("admin=" + user);

        List<Currency> currencies = currencyService.listCurrencies();
        List<Country> countries = countryService.listCountries();
        List<Account> accounts = accountService.listAccounts();
        model.addAttribute("currencies", currencies);
        model.addAttribute("countries", countries);
        model.addAttribute("accounts", accounts);

        //We dont want to allow changing admin user username or password
        model.addAttribute("isUsernameAndPasswordEditable", !isAdmin(user));

        return "admin/user";
    }

    public static boolean isAdmin(User user) {
        return user.getUserRoles().stream().anyMatch(userRole -> userRole.getRole().getName().equals("ROLE_ADMIN"));
    }

    @RequestMapping(path = "/admin/user/update", method = RequestMethod.POST)
    public String updateUser(@Valid User user, @RequestParam("id") String id, @RequestParam(value = "newPassword", required = false) String newPassword, @RequestParam("newUsername") String newUsername, @RequestParam("newEmail") String newEmail, BindingResult bindingResult, Model model){

        LOGGER.info("user update");

        List<Currency> currencies = currencyService.listCurrencies();
        List<Country> countries = countryService.listCountries();
        List<Account> accounts = accountService.listAccounts();
        model.addAttribute("currencies", currencies);
        model.addAttribute("countries", countries);
        model.addAttribute("accounts", accounts);

        Long userId = Long.parseLong(id);
        //We dont want to allow changing admin user username or password
        User userTemp = userService.findById(userId);
        model.addAttribute("isUsernameAndPasswordEditable", !isAdmin(user));


        //Field validation
        if(!StringUtils.isEmpty(newPassword)){
            if(StringUtils.length(newPassword)<3){
                bindingResult.rejectValue("password","user.field.minSize");
            }
        }
        if(!newUsername.equals(user.getUsername())) {
            if (!StringUtils.isEmpty(newUsername)) {
                if (StringUtils.length(newUsername) < 3) {
                    bindingResult.rejectValue("username", "user.field.minSize");
                }
                if (userService.findByUsername(newUsername) != null) {
                    bindingResult.rejectValue("username", "user.username.unique");
                }
            } else {
                bindingResult.rejectValue("username", "user.field.notEmpty");
            }
        }
        if(!newEmail.equals(user.getEmail())) {
            if (!StringUtils.isEmpty(newEmail)) {
                EmailValidator validator = EmailValidator.getInstance();
                if (StringUtils.length(newEmail) < 3) {
                    bindingResult.rejectValue("email", "user.field.minSize");
                }
                if (!validator.isValid(newEmail)) {
                    bindingResult.rejectValue("email", "user.email.format");
                }
                if (userService.findByEmail(newEmail) != null) {
                    bindingResult.rejectValue("email", "user.email.unique");
                }
            } else {
                bindingResult.rejectValue("email", "user.field.notEmpty");
            }
        }


        if(bindingResult.hasErrors()){
            return "admin/user";
        }


        User newUser = userService.findById(userId);
        newUser.setEmail(newEmail);
        newUser.setUsername(newUsername);
        newUser.setName(user.getName());
        newUser.setSurname(user.getSurname());
        newUser.setCountryOfInstallation(user.getCountryOfInstallation());
        newUser.setCurrency(user.getCurrency());
        newUser.setAccount(user.getAccount());
        newUser.setEnabled(user.isEnabled());

        if(!StringUtils.isEmpty(newPassword)) {
            newUser.setPassword(encoder.encode(newPassword));
        }
        userService.save(newUser);

        model.addAttribute("message", messageSource.getMessage("userUpdatedMessage", null, Locale.getDefault()));
        model.addAttribute("user", newUser);


        return "admin/user";
    }


}
