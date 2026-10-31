package com.skillbridge.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            RestAuthenticationEntryPoint entryPoint,
                                            RestAccessDeniedHandler deniedHandler,
                                            CorsConfigurationSource corsConfigurationSource,
                                            @Value("${skillbridge.rate-limit.auth.max-requests:20}") int authMaxRequests,
                                            @Value("${skillbridge.rate-limit.auth.window-seconds:60}") long authWindowSeconds)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)          // bearer tokens, no cookies, no session
                .cors(c -> c.configurationSource(corsConfigurationSource))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler))

                .oauth2ResourceServer(o -> o
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(entryPoint)      // keeps garbage-token 401s in our JSON format
                        .accessDeniedHandler(deniedHandler))

                // Brute-force protection for register and login, before any authentication work happens.
                .addFilterBefore(
                        new AuthRateLimitFilter(authMaxRequests, authWindowSeconds, System::currentTimeMillis),
                        UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    /**
     * CORS is deliberate: only the origins listed in CORS_ALLOWED_ORIGINS (comma separated) may call the API
     * from a browser. The default is none. Tokens are sent in the Authorization header, not in cookies,
     * so credentials are not allowed.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${skillbridge.cors.allowed-origins:}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("X-Total-Count", "Retry-After"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    /**
     * Passwords are checked in AuthService, not by Spring's username/password flow.
     * This bean only stops Boot creating its default user with a random password in the log.
     */
    @Bean
    UserDetailsService noUsersYet() {
        return username -> {
            throw new UsernameNotFoundException("Password login is not available yet");
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}