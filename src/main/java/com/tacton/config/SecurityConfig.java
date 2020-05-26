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

package com.tacton.config;


import com.tacton.services.UserSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;


@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {


    @Autowired
    BCryptPasswordEncoder encoder;

    @Autowired
    UserSecurityService userSecurityService;


    // Allowed for all
    private static final String[] PublicMatchers = {
            "/webjars/**",
            "/css/**",
            "/js/**",
            "/img/**",
            "/fonts/**",
            "/webfonts/**",
            "/products/**",
            "/images/**",
            "/",
            "/configurator",
            "/configure",
            "/configure-needs/**",
            "/templates",
            "/vis/**",
            "/shop",
            "/shop/**",
            "/admin-assets/**",
            "/accessDenied"

    };


    //only allowed for non-authenticated
    private static final String[] AnonymousMatchers = {
            "/login",
            "/login/**",
            "/logout",
            "/logout/**",
            "/register",
    };

    //only allowed for users with admin role
    private static final String[] AdminMatchers = {
            "/admin",
            "/admin/**",
    };


    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return new CustomLoginSuccessHandler();
    }

    @Bean
    public AuthenticationFailureHandler failureHandler() {
        return new CustomAuthenticationFailureHandler();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler(){
        return new CustomAccessDeniedHandler();
    }


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .cors().disable()
                .authorizeRequests()
                .antMatchers(AdminMatchers).hasRole("ADMIN")
                .antMatchers(PublicMatchers).permitAll()
                //to access this urls user has to be not loggedin
                .antMatchers(AnonymousMatchers).anonymous()
                //any other request, must be authenticated (role is irrevelant)
                //.anyRequest().authenticated()
                //any other request must be authenticated and role=USER
                .anyRequest().hasRole("USER")
                .and()
                .exceptionHandling().accessDeniedHandler(accessDeniedHandler())
                .and()
                .formLogin().loginPage("/login").successHandler(successHandler()).failureHandler(failureHandler()).failureUrl("/login/error").permitAll()
                .and()
                .logout().logoutRequestMatcher(new AntPathRequestMatcher("/logout")).logoutSuccessUrl("/logout/success").invalidateHttpSession(true).deleteCookies("remember-me").permitAll()
                .and()
                .rememberMe();

        http.headers().frameOptions().sameOrigin();
    }


    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/h2/**");
    }


    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception{
        auth
                .userDetailsService(userSecurityService)
                .passwordEncoder(encoder);
    }






}
