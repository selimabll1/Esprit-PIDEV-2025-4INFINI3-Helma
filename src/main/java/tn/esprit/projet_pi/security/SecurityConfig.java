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

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ✅ PUBLIC
                        .requestMatchers(HttpMethod.POST, "/api/loans/simulate").permitAll()
                        .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()

                        // 👑 ADMIN seulement
                        .requestMatchers(HttpMethod.PUT, "/api/loans/*/approve").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/loans/*/reject").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/loans/*/ai-decision").hasAuthority("ROLE_ADMIN")

                        // 👑 ADMIN + COMPLIANCE
                        .requestMatchers(HttpMethod.GET, "/api/loans").hasAnyAuthority("ROLE_ADMIN", "ROLE_COMPLIANCE")
                        .requestMatchers(HttpMethod.POST, "/api/loans/*/ml-predict").hasAnyAuthority("ROLE_ADMIN", "ROLE_COMPLIANCE")
                        .requestMatchers(HttpMethod.POST, "/api/loans/*/markov-predict").hasAnyAuthority("ROLE_ADMIN", "ROLE_COMPLIANCE")

                        // 📊 ADMIN + COMPLIANCE + INVESTOR
                        .requestMatchers(HttpMethod.GET, "/api/loans/statistics").hasAnyAuthority("ROLE_ADMIN", "ROLE_COMPLIANCE", "ROLE_INVESTOR")

                        // 🧑 YOUTH + ADMIN — créer et payer
                        .requestMatchers(HttpMethod.POST, "/api/loans").hasAnyAuthority("ROLE_YOUTH", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/loans/payments").hasAnyAuthority("ROLE_YOUTH", "ROLE_ADMIN")

                        // 🧑 YOUTH + ADMIN + COMPLIANCE — voir ses prêts
                        .requestMatchers(HttpMethod.GET, "/api/loans/*").hasAnyAuthority("ROLE_YOUTH", "ROLE_ADMIN", "ROLE_COMPLIANCE")
                        .requestMatchers(HttpMethod.GET, "/api/loans/user/*").hasAnyAuthority("ROLE_YOUTH", "ROLE_ADMIN", "ROLE_COMPLIANCE")

                        // Tout le reste : connecté
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }
}