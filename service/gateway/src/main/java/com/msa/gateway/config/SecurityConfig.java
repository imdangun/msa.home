package com.msa.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity // WebFlux 환경에서 Security 활성화
public class SecurityConfig {
    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {

        // 1. JWT Resource Server 활성화
        // 요청 헤더의 Bearer 토큰을 검증하고 Principal로 변환합니다. (yml의 issuer-uri 사용)
        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        // 2. 접근 권한 설정
        http.authorizeExchange(exchanges ->
                exchanges
                        // 상태 점검 엔드포인트는 인증 없이 모든 접근을 허용합니다. (permitAll())
                        .pathMatchers("/actuator/health").permitAll()
                        // 그 외 모든 요청 (/**)은 인증된 사용자만 허용합니다. (authenticated())
                        .anyExchange().authenticated()
        );

        // 3. 웹 세션/CSRF 비활성화 (MSA API Gateway의 표준)
        // Gateway는 세션이나 CSRF 토큰을 관리하지 않으므로 비활성화합니다.
        http.csrf(csrf -> csrf.disable());

        return http.build();
    }
}