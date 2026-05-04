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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

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
         http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable());
        http.sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        http.authorizeHttpRequests(auth -> auth
                // Important for browser preflight requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/goals/**").permitAll()
                .requestMatchers("/api/admin/goals/**").permitAll()
                .requestMatchers("/api/stats/**").permitAll()
                .requestMatchers("/api/vouchers/**").permitAll()
                .requestMatchers("/api/deposits/**").permitAll()
                // Public endpoints
                .requestMatchers(
                        "/api/auth/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers("/api/goals/**").permitAll()
                .requestMatchers("/api/admin/goals/**").permitAll()
                .requestMatchers("/api/stats/**").permitAll()
                .requestMatchers("/api/vouchers/**").permitAll()
                .requestMatchers("/api/deposits/**").permitAll()

                // Public campaign browsing
                .requestMatchers(HttpMethod.GET,
                        "/api/crowdfunding/campaigns",
                        "/api/crowdfunding/campaigns/*"
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
                        "/api/crowdfunding/admin/payments/**"
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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}