package tn.esprit.projet_pi.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/loans/simulate").permitAll()
                        .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()

                        .requestMatchers(HttpMethod.PUT,  "/api/loans/*/approve").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT,  "/api/loans/*/reject").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/loans/*/ai-decision").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/loans/*/multi-agent-decision").hasAuthority("ROLE_ADMIN")

                        .requestMatchers(HttpMethod.GET,  "/api/loans").hasAnyAuthority("ROLE_ADMIN", "ROLE_COMPLIANCE")
                        .requestMatchers(HttpMethod.GET,  "/api/loans/early-warnings").hasAnyAuthority("ROLE_ADMIN", "ROLE_COMPLIANCE")
                        .requestMatchers(HttpMethod.POST, "/api/loans/*/ml-predict").hasAnyAuthority("ROLE_ADMIN", "ROLE_COMPLIANCE")
                        .requestMatchers(HttpMethod.POST, "/api/loans/*/markov-predict").hasAnyAuthority("ROLE_ADMIN", "ROLE_COMPLIANCE")

                        .requestMatchers(HttpMethod.GET,  "/api/loans/statistics").hasAnyAuthority("ROLE_ADMIN", "ROLE_COMPLIANCE", "ROLE_INVESTOR")

                        .requestMatchers(HttpMethod.POST, "/api/loans").hasAuthority("ROLE_YOUTH_BENEFICIARY")
                        .requestMatchers(HttpMethod.POST, "/api/loans/payments").hasAuthority("ROLE_YOUTH_BENEFICIARY")

                        .requestMatchers(HttpMethod.GET, "/api/loans/*/generate-contract").hasAnyAuthority("ROLE_YOUTH_BENEFICIARY", "ROLE_ADMIN", "ROLE_COMPLIANCE")
                        .requestMatchers(HttpMethod.GET, "/api/loans/*/schedule").hasAnyAuthority("ROLE_YOUTH_BENEFICIARY", "ROLE_ADMIN", "ROLE_COMPLIANCE")
                        .requestMatchers(HttpMethod.GET, "/api/loans/*/summary").hasAnyAuthority("ROLE_YOUTH_BENEFICIARY", "ROLE_ADMIN", "ROLE_COMPLIANCE")
                        .requestMatchers(HttpMethod.GET, "/api/loans/*").hasAnyAuthority("ROLE_YOUTH_BENEFICIARY", "ROLE_ADMIN", "ROLE_COMPLIANCE")
                        .requestMatchers(HttpMethod.GET, "/api/loans/user/*").hasAnyAuthority("ROLE_YOUTH_BENEFICIARY", "ROLE_ADMIN", "ROLE_COMPLIANCE")
                        .requestMatchers(HttpMethod.GET, "/api/loans/payments/*").hasAnyAuthority("ROLE_YOUTH_BENEFICIARY", "ROLE_ADMIN", "ROLE_COMPLIANCE")
                        .requestMatchers(HttpMethod.GET, "/api/users/*/financial-health").hasAnyAuthority("ROLE_YOUTH_BENEFICIARY", "ROLE_ADMIN", "ROLE_COMPLIANCE")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200", "http://localhost:4300"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }
}
