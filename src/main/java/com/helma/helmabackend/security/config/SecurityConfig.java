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

    private static final String[] YOUTH_AUTHORITIES = {
            "YOUTH_BENEFICIARY",
            "ROLE_YOUTH_BENEFICIARY"
    };

    private static final String[] INVESTOR_AUTHORITIES = {
            "INVESTOR",
            "ROLE_INVESTOR"
    };

    private static final String[] ADMIN_COMPLIANCE_AUTHORITIES = {
            "ADMIN",
            "COMPLIANCE",
            "ROLE_ADMIN",
            "ROLE_COMPLIANCE"
    };

    private static final String[] YOUTH_ADMIN_COMPLIANCE_AUTHORITIES = {
            "YOUTH_BENEFICIARY",
            "ADMIN",
            "COMPLIANCE",
            "ROLE_YOUTH_BENEFICIARY",
            "ROLE_ADMIN",
            "ROLE_COMPLIANCE"
    };

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());

        http.sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        http.authorizeHttpRequests(auth -> auth
                // Important for browser preflight requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Public endpoints
                .requestMatchers(
                        "/api/auth/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**"
                ).permitAll()

                // Stripe webhooks must be public, but verified by Stripe-Signature.
                .requestMatchers(HttpMethod.POST, "/api/crowdfunding/stripe/webhook").permitAll()

                // Public campaign browsing
                .requestMatchers(HttpMethod.GET,
                        "/api/crowdfunding/campaigns",
                        "/api/crowdfunding/campaigns/*",
                        "/api/crowdfunding/public-campaigns",
                        "/api/crowdfunding/public-campaigns/*",
                        "/api/crowdfunding/public-campaigns/*/documents/*/content"

                ).permitAll()

                // Current user profile
                .requestMatchers(HttpMethod.GET, "/api/users/me/profile").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/users/me/profile").authenticated()

                // Admin + compliance endpoints
                .requestMatchers(
                        "/api/crowdfunding/application-raises/admin",
                        "/api/crowdfunding/application-raises/admin/**",
                        "/api/crowdfunding/application-raises/*/admin/**",
                        "/api/crowdfunding/admin/pledges",
                        "/api/crowdfunding/admin/pledges/**",
                        "/api/crowdfunding/admin/payments",
                        "/api/crowdfunding/admin/payments/**",
                        "/api/crowdfunding/admin/campaign-pages",
                        "/api/crowdfunding/admin/campaign-pages/**"
                ).hasAnyAuthority(ADMIN_COMPLIANCE_AUTHORITIES)

                // Investor endpoints
                .requestMatchers(
                        "/api/crowdfunding/campaigns/*/pledges",
                        "/api/crowdfunding/my-pledges/**",
                        "/api/crowdfunding/my-payments/**"
                ).hasAnyAuthority(INVESTOR_AUTHORITIES)

                // Youth draft wizard endpoints
                .requestMatchers(HttpMethod.POST, "/api/crowdfunding/application-raises/drafts")
                .hasAnyAuthority(YOUTH_AUTHORITIES)

                .requestMatchers(HttpMethod.PATCH, "/api/crowdfunding/application-raises/*/steps/contact")
                .hasAnyAuthority(YOUTH_AUTHORITIES)

                .requestMatchers(HttpMethod.PATCH, "/api/crowdfunding/application-raises/*/steps/type")
                .hasAnyAuthority(YOUTH_AUTHORITIES)

                .requestMatchers(HttpMethod.PATCH, "/api/crowdfunding/application-raises/*/steps/details")
                .hasAnyAuthority(YOUTH_AUTHORITIES)

                .requestMatchers(HttpMethod.POST, "/api/crowdfunding/application-raises/*/submit")
                .hasAnyAuthority(YOUTH_AUTHORITIES)

                // General crowdfunding access for youth/admin/compliance
                .requestMatchers("/api/crowdfunding/**")
                .hasAnyAuthority(YOUTH_ADMIN_COMPLIANCE_AUTHORITIES)

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