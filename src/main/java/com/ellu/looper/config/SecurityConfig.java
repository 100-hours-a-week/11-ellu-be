package com.ellu.looper.config;

import com.ellu.looper.jwt.JwtAuthenticationFilter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable()) // CSRF 비활성화
        .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정 적용
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/auth/**",
                        "/error",
                        "/actuator/**",
                        "/auth/kakao/callback",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/ai-callback/**",
                        "/sse/**",
                        "/admin/**",
                        "/connect/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll() // Preflight 요청 허용
                    .requestMatchers(HttpMethod.GET, "/projects/*/tasks/preview")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/projects/*/schedules")
                    .permitAll()
                    .anyRequest()
                    .authenticated() // 나머지는 인증 필요
            )
        .addFilterBefore(
            jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // JWT 필터 등록

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedOriginPatterns(
        List.of("http://localhost:3000", "https://looper.my", "https://dev.looper.my"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true); // 쿠키 인증 허용
    configuration.setMaxAge(3600L); // 1시간

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
