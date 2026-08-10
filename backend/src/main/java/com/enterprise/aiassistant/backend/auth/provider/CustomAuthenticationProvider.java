package com.enterprise.aiassistant.backend.auth.provider;


import com.enterprise.aiassistant.backend.auth.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        String username = authentication.getName();
        Object credentials = authentication.getCredentials();

        if (credentials == null) {
            throw new BadCredentialsException("Invalid credentials");
        }
        String rawPassword = credentials.toString();

        UserDetails user;
        try {
            user = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException ex) {
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!user.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }
        if (!user.isAccountNonLocked()) {
            throw new LockedException("Account is locked");
        }
        if (!user.isCredentialsNonExpired()) {
            throw new CredentialsExpiredException("Credentials expired");
        }

        return new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities()
        );
    }
    @Override
    public boolean supports(Class<?> authentication){

        return UsernamePasswordAuthenticationToken
                .class
                .isAssignableFrom(authentication);
    }
}