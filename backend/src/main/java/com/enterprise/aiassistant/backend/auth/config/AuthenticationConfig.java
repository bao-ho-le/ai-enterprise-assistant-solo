package com.enterprise.aiassistant.backend.auth.config;

import com.enterprise.aiassistant.backend.auth.provider.CustomAuthenticationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;

@Configuration
@RequiredArgsConstructor
public class AuthenticationConfig {


    private final CustomAuthenticationProvider provider;



    @Bean
    public AuthenticationManager authenticationManager(){

        return new ProviderManager(
                provider
        );
    }

}