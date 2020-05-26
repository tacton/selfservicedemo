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

package com.tacton.validators;

import com.tacton.entities.User;
import com.tacton.repositories.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;


@Service
public class UserRegistrationFormValidator implements Validator {


    @Autowired
    UserRepository userRepository;


    @Override
    public boolean supports(Class<?> aClass) {
        return User.class.equals(aClass);
    }

    @Override
    public void validate(Object o, Errors errors) {

        User user = (User) o;

        if(userRepository.findByUsername(user.getUsername()) != null) {
            errors.rejectValue("username","user.username.unique");
        }

        if(userRepository.findByEmail(user.getEmail()) != null) {
            errors.rejectValue("email","user.email.unique");
        }

        if(StringUtils.isEmpty(user.getPassword())){
            errors.rejectValue("password","user.field.notEmpty");
        }
        else{
            if(StringUtils.length(user.getPassword())<3){
                errors.rejectValue("password","user.password.minSize");
            }
        }
        if(StringUtils.isEmpty(user.getCountryOfInstallation())){
            errors.rejectValue("country","user.field.notEmpty");
        }
        if(StringUtils.isEmpty(user.getCurrency())){
            errors.rejectValue("currency","user.field.notEmpty");
        }

    }
}
