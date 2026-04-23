package com.helma.helmabackend.security.config;

import com.helma.helmabackend.security.jwt.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());

        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/api/auth/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                ).permitAll()

                .requestMatchers(HttpMethod.GET,
                        "/api/crowdfunding/campaigns",
                        "/api/crowdfunding/campaigns/*"
                ).permitAll()

                .requestMatchers(HttpMethod.GET, "/api/users/me/profile").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/users/me/profile").authenticated()

                .requestMatchers(
                        "/api/crowdfunding/admin/pledges/**",
                        "/api/crowdfunding/admin/payments/**",
                        "/api/crowdfunding/application-raises/*/admin/**"
                ).hasAnyRole("ADMIN", "COMPLIANCE")

                .requestMatchers(
                        "/api/crowdfunding/campaigns/*/pledges",
                        "/api/crowdfunding/my-pledges/**",
                        "/api/crowdfunding/my-payments/**"
                ).hasRole("INVESTOR")

                .requestMatchers("/api/crowdfunding/**")
                .hasAnyRole("YOUTH_BENEFICIARY", "ADMIN", "COMPLIANCE")

                .anyRequest().authenticated()
        );

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}