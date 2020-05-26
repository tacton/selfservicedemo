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

import com.tacton.controllers.AdminController;
import com.tacton.entities.User;
import com.tacton.repositories.UserRepository;
import com.tacton.services.cpq.CartCreateException;
import com.tacton.services.cpq.CartService;
import com.tacton.services.cpq.WrongAccountException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
public class UserSecurityService implements UserDetailsService {


    @Autowired
    private UserRepository userRepository;

    @Autowired
    CartService cartService;

    @Autowired
    UserService userService;

    private final Logger LOGGER = LoggerFactory.getLogger(UserSecurityService.class);

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        LOGGER.info("authenticating");

        User user = userRepository.findByUsername(username);
        if (null == user) {
            throw new UsernameNotFoundException("Username " + username + " not found");
        }

        if (user.isEnabled()==true && !AdminController.isAdmin(user)) {
            try {
                userService.setActiveCartOrCreateOne(user);
            } catch (CartCreateException ex) {
                LOGGER.warn("Cannot create cart, wrong account.", ex);
                throw new WrongAccountException(ex.getMessage());
            }
        }

        return user;
    }
}
