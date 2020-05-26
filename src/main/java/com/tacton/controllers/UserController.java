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

import com.tacton.entities.Role;
import com.tacton.entities.User;
import com.tacton.entities.UserRole;
import com.tacton.entities.cpqresponse.Account;
import com.tacton.entities.cpqresponse.Country;
import com.tacton.entities.cpqresponse.Currency;
import com.tacton.services.RoleService;
import com.tacton.services.UserService;
import com.tacton.services.cpq.*;
import com.tacton.validators.UserRegistrationFormValidator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Controller
public class UserController {

    private final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    @Autowired
    BCryptPasswordEncoder encoder;

    @Autowired
    private UserService userService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private CountryService countryService;

    @Autowired
    UserRegistrationFormValidator userRegistrationFormValidator;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private RoleService roleService;


    @Value("${css_default_country}")
    private String css_default_country;

    @Value("${css_default_currency}")
    private String css_default_currency;




    @RequestMapping("/myProfile")
    public String myProfile(Model model) {

        User userFromAuthentication = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userService.findById(userFromAuthentication.getId());
        List<Account> accounts = accountService.listAccounts();
        List<Currency> currencies = currencyService.listCurrencies();
        List<Country> countries = countryService.listCountries();
        model.addAttribute("user", user);
        model.addAttribute("accounts", accounts);
        model.addAttribute("currencies", currencies);
        model.addAttribute("countries", countries);


        // Get the name of the user account
        Account userAccount = accounts.stream()
                .filter(account -> user.getAccount().equals(account.getAccountId()))
                .findAny()
                .orElse(null);
        if(userAccount == null) {
            throw new WrongAccountException("wrongUserAccountMessage");
        }

        model.addAttribute("userAccount", userAccount);

        return "myProfile";

    }


    @RequestMapping(path = "/updateMyProfile", method = RequestMethod.POST)
    public String updateMyProfile(@Valid User userFromForm, @RequestParam("newPassword") String newPassword, @RequestParam("newUsername") String newUsername, @RequestParam("newEmail") String newEmail, BindingResult bindingResult, Model model) {

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        List<Account> accounts = accountService.listAccounts();
        List<Currency> currencies = currencyService.listCurrencies();
        List<Country> countries = countryService.listCountries();
        model.addAttribute("user", userFromForm);
        model.addAttribute("accounts", accounts);
        model.addAttribute("currencies", currencies);
        model.addAttribute("countries", countries);

        // Get the name of the user account
        Account userAccount = accounts.stream()
                .filter(account -> user.getAccount().equals(account.getAccountId()))
                .findAny()
                .orElse(null);
        if(userAccount == null) {
            throw new WrongAccountException("wrongUserAccountMessage");
        }

        model.addAttribute("userAccount", userAccount);


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
            return "myProfile";
        }

        if(!StringUtils.isEmpty(newPassword)) {
            user.setPassword(encoder.encode(newPassword));
        }

        user.setEmail(newEmail);
        user.setUsername(newUsername);
        user.setName(userFromForm.getName());
        user.setSurname(userFromForm.getSurname());
        user.setCurrency(userFromForm.getCurrency());
        user.setCountryOfInstallation(userFromForm.getCountryOfInstallation());

        userService.save(user);

        model.addAttribute("user", user);

        //We want to update authenticated user in context
        Authentication auth = new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        return "myProfile";
    }



    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public String registerForm(Model model){

        User user = new User();
       // user.setCountryOfInstallation(css_default_country);
       // user.setCurrency(css_default_currency);
        model.addAttribute("user", user);

        List<Currency> currencies = currencyService.listCurrencies();
        List<Country> countries = countryService.listCountries();
        model.addAttribute("currencies", currencies);
        model.addAttribute("countries", countries);

        return "registrationForm";
    }


    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public String register(@Valid User user, BindingResult bindingResult, Model model){

        List<Currency> currencies = currencyService.listCurrencies();
        List<Country> countries = countryService.listCountries();
        model.addAttribute("currencies", currencies);
        model.addAttribute("countries", countries);


        userRegistrationFormValidator.validate(user,bindingResult);

        if(bindingResult.hasErrors()){
            return "registrationForm";
        }

        Set<UserRole> userRoles = new HashSet<>();
        userRoles.add(new UserRole(roleService.findByName("ROLE_USER"), user));

        user.setPassword(encoder.encode(user.getPassword()));

        userService.createUser(user, userRoles);

        model.addAttribute("message", messageSource.getMessage("registrationCompletedMessage", null, Locale.getDefault()));
        return "notificationPage";
    }


    @ExceptionHandler(WrongAccountException.class)
    public ModelAndView handleCartCreateException(HttpServletRequest request, Exception ex, RedirectAttributes redirectAttributes){
        request.getSession().invalidate();

        String message = messageSource.getMessage(ex.getMessage(), null, Locale.getDefault());

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("header", "Error");
        modelAndView.addObject("message", message);

        modelAndView.setViewName("notificationPage");

        return modelAndView;
    }

}
