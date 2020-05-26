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

import com.tacton.services.NeedsParam;
import com.tacton.services.cpq.WrongAccountException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Controller
public class HomeController {


    @Autowired
    private MessageSource messageSource;

    @Autowired
    private NeedsParam needsParam;

    @Value("${leadgen_needs_params}")
    private String leadgen_needs_params;


    @RequestMapping(path = "/")
    public String getIndex(Model model, HttpSession session) throws IOException {
        Optional<Supplier<Stream<String>>> valueSupplier = needsParam.getUsedList(leadgen_needs_params);

        if(valueSupplier.isPresent()) {
            List<String> needsValues = (List<String>) session.getAttribute("needsValueList");
            if(needsValues == null) {
                needsValues = valueSupplier.get().get().collect(Collectors.toList());
                session.setAttribute("needsValueList", needsValues);
            }

            model.addAttribute("needsValueList", needsValues);
        }

        return "index";
    }


    @RequestMapping("/login")
    public String login(HttpServletRequest request, HttpSession session, Model model){

        String referer = request.getHeader("Referer");
        request.getSession().setAttribute("url_prior_login", referer);

        return "login";
    }


    @RequestMapping("/login/error")
    public String loginError(HttpSession session, Model model){

        AuthenticationException authenticationException = (AuthenticationException) session.getAttribute("SPRING_SECURITY_LAST_EXCEPTION");
        if (authenticationException instanceof BadCredentialsException) {
            model.addAttribute("error", messageSource.getMessage("BadCredentialsExceptionMessage", null, Locale.getDefault()));
        }
        else if(authenticationException instanceof DisabledException){
            model.addAttribute("error", messageSource.getMessage("DisabledExceptionMessage", null, Locale.getDefault()));
        }
        else if (authenticationException instanceof WrongAccountException) {
            model.addAttribute("error", messageSource.getMessage(authenticationException.getMessage(), null, Locale.getDefault()));
        }

        return "login";
    }


    @RequestMapping("/accessDenied")
    public String accessDenied(HttpSession session, Model model){

        model.addAttribute("error", messageSource.getMessage("accessDeniedMessage", null, Locale.getDefault()));

        return "errorPage";
    }


    @RequestMapping("/logout/success")
    public String logoutSuccess(HttpSession session, Model model){

        model.addAttribute("message", messageSource.getMessage("logoutSuccessMessage", null, Locale.getDefault()));

        return "notificationPage";
    }



}
