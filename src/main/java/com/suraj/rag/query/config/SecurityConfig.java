package com.suraj.rag.query.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http, SecurityProperties securityProperties) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable);
        if (securityProperties.enabled()) {
            http.authorizeExchange(
                            exchanges ->
                                    exchanges
                                            .pathMatchers(
                                                    "/actuator/health/**",
                                                    "/v3/api-docs/**",
                                                    "/swagger-ui.html",
                                                    "/swagger-ui/**")
                                            .permitAll()
                                            .anyExchange()
                                            .authenticated())
                    .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        } else {
            http.authorizeExchange(exchanges -> exchanges.anyExchange().permitAll());
        }
        return http.build();
    }
}
