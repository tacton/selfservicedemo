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


import com.tacton.entities.User;
import com.tacton.entities.UserRole;
import com.tacton.entities.cpqresponse.CartAttributes;
import com.tacton.repositories.RoleRepository;
import com.tacton.repositories.UserRepository;
import com.tacton.services.cpq.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);


    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    CartService cartService;




    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User findById(Long id){
        return userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id.toString()));
    }

    public User findByEmail (String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> findAll() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "Id") );
    }

    public User createUser(User user, Set<UserRole> userRoles) {
        LOGGER.info("Creating user: " + user.getUsername());
        User localUser = userRepository.findByUsername(user.getUsername());

        if(localUser != null) {
            LOGGER.info("user {} already exists. Nothing will be done.", user.getUsername());
        } else {
            for (UserRole ur : userRoles) {
                roleRepository.save(ur.getRole());
            }

            user.getUserRoles().addAll(userRoles);

            user.setCreationDate(new Date());
            localUser = userRepository.save(user);
        }

        return localUser;
    }



    public User save(User user) {
        return userRepository.save(user);
    }




    public void setActiveCartOrCreateOne(User user){

        //Looking for qapplicable carts to be set as active
        List<CartAttributes> cartsAll = cartService.findUserCarts(user.getId().toString());

        //Filtering out deleted and firm quote carts
        List<CartAttributes> carts = cartService.filterCartsCreated(cartsAll);

        if (carts.isEmpty()) {
            LOGGER.info("No carts, creating one.");
            cartService.createNewCartAttributesForUser(user);

            List<CartAttributes> allCarts = cartService.findUserCarts(user.getId().toString());

            //Filtering out deleted and firm quote carts
            List<CartAttributes> cartsFiltered = cartService.filterCartsCreated(allCarts);

            CartAttributes latestCartAttributes = cartService.getLatestCreatedCartAttributes(cartsFiltered);

            user.setActiveCartId(latestCartAttributes.getExternalId());
            user.setActiveCart(latestCartAttributes);
            user.setCarts(cartsFiltered);
            userRepository.save(user);
        } else {
            user.setCarts(carts);
        }

        cartsAll.removeAll(carts);

        if (user.getActiveCartId() == null || user.getActiveCartId().isEmpty() ||
                cartsAll.parallelStream().map(CartAttributes::getExternalId).collect(Collectors.toList()).contains(user.getActiveCartId()) ||
                !carts.contains(user.getActiveCartId()) ) {

            LOGGER.info("No Active cart or Active cart equals requested firm or deleted setting one.");
            List<CartAttributes> allCarts = cartService.findUserCarts(user.getId().toString());

            //Filtering out deleted and firm quote carts
            List<CartAttributes> cartsFiltered = cartService.filterCartsCreated(allCarts);

            CartAttributes latestCartAttributes = cartService.getLatestCreatedCartAttributes(cartsFiltered);

            user.setActiveCartId(latestCartAttributes.getExternalId());
            user.setActiveCart(latestCartAttributes);
            userRepository.save(user);
        }

        //ActiveCart is @Transient so it needs to be fetched based on user.activeCardId
        if (user.getActiveCart() == null) {
            CartAttributes activeCart = user.getCarts().stream()
                    .filter(cart -> user.getActiveCartId().equals(cart.getExternalId()))
                    .findAny()
                    .orElse(null);
            user.setActiveCart(activeCart);
        }

    }


}
