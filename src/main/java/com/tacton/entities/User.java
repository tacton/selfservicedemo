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

package com.tacton.entities;

import com.tacton.entities.cpqresponse.CartAttributes;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.*;

@Entity
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotEmpty
    @Size(min=3, message = "{user.field.minSize}")
    private String username;

    private String password;

    @NotEmpty
    @Email(message = "{user.email.format}")
    private String email;

    @NotEmpty
    @Size(min=3, message = "{user.field.minSize}")
    private String name;

    @NotEmpty
    @Size(min=3, message = "{user.field.minSize}")
    private String surname;

    private String phone;

    @NotEmpty
    private String countryOfInstallation;

    @NotEmpty
    private String currency;

    private String account;

    private boolean enabled;

    private String activeCartId;

    private Date creationDate;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<UserRole> userRoles = new HashSet<>();

    @Transient
    private List<CartAttributes> carts = new ArrayList<>();

    @Transient
    private CartAttributes activeCart;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Set<UserRole> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(Set<UserRole> userRoles) {
        this.userRoles = userRoles;
    }


    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean getEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
        userRoles.forEach(userRole -> grantedAuthorities.add(new Authority(userRole.getRole().getName())));
        return grantedAuthorities;
    }


    public String getActiveCartId() {
        return activeCartId;
    }

    public void setActiveCartId(String activeCartId) {
        this.activeCartId = activeCartId;
    }


    public List<CartAttributes> getCarts() {
        return carts;
    }

    public void setCarts(List<CartAttributes> carts) {
        this.carts = carts;
    }


    public CartAttributes getActiveCart() {
        return activeCart;
    }

    public void setActiveCart(CartAttributes activeCart) {
        this.activeCart = activeCart;
    }


    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", phone='" + phone + '\'' +
                ", countryOfInstallation='" + countryOfInstallation + '\'' +
                ", currency='" + currency + '\'' +
                ", account='" + account + '\'' +
                ", enabled=" + enabled +
                ", activeCartId='" + activeCartId + '\'' +
                ", userRoles=" + userRoles +
                ", carts=" + carts +
                ", activeCart=" + activeCart +
                '}';
    }

    public String getCountryOfInstallation() {
        return countryOfInstallation;
    }

    public void setCountryOfInstallation(String countryOfInstallation) {
        this.countryOfInstallation = countryOfInstallation;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }
}
