package org.tikcoin.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.tikcoin.exception.CustomAccessDeniedHandler;
import org.tikcoin.security.JwtFilter;

import java.util.List;

@EnableWebSecurity
@RequiredArgsConstructor
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    private final JwtFilter jwtAuthenticationFilter;
    private final AuthenticationProvider authenticationProvider;
    private final LogoutHandler logoutHandler;
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfiguration.class);


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        logger.debug("Configuring security filter chain");
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sessionManagement ->
                        sessionManagement
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
//                .requiresChannel(channel ->
//                        channel.anyRequest().requiresSecure()
//                )
                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                .requestMatchers(
                                        "/api/auth/tiktok/authorize",
                                        "/api/auth/tiktok/callback",
                                        "/api/auth/login",
                                        "/api/webhook/paystack",
                                        "/swagger-ui.html",
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**",
                                        "/webjars/**",
                                        "/ws/**",
                                        "/api/admin/rate",
                                        "/api/admin/rate/**",
                                        "/api/admin/orders",
                                        "/api/admin/orders/**",
                                        "/api/admin/transactions",
                                        "/api/orders",
                                        "/api/orders/**",
                                        "/api/notifications",
                                        "/api/notifications/**"
                                ).permitAll()
                                .anyRequest().authenticated()

                )
                .logout((logout) ->
                        logout
                                .logoutUrl("/logout")
                                .addLogoutHandler(logoutHandler)
                                .logoutSuccessUrl("/login?logout")
                                .invalidateHttpSession(true)
                                .deleteCookies("JSESSIONID")
                                .permitAll()
                                .logoutSuccessHandler((request, response, authentication) ->
                                        SecurityContextHolder.clearContext())
                )
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling
                                .authenticationEntryPoint((request, response, authException) -> {
                                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                    response.getWriter().write("{\"message\": \"" + authException.getMessage() + "\", \"status\": false}");
                                })
                                .accessDeniedHandler(new CustomAccessDeniedHandler())
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
