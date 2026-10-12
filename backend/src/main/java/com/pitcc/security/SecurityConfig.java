package com.pitcc.security;

import com.pitcc.repository.UserRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

  public static final String AUTHENTICATION_REQUIRED = "Autenticação necessária.";
  public static final String ACCESS_DENIED = "Você não tem permissão para esta ação.";

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, JwtService jwtService, UserRepository userRepository, ApiErrorWriter errors)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        .httpBasic(basic -> basic.disable())
        .formLogin(form -> form.disable())
        .logout(logout -> logout.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.GET, "/api/health", "/api/health/ready")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/users")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/login")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/catalog/**")
                    .permitAll()
                    .requestMatchers("/api/users/me/**")
                    .authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/users/*/profile")
                    .permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers("/error")
                    .permitAll()
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            handler ->
                handler
                    .authenticationEntryPoint(
                        (_, response, _) ->
                            errors.write(response, HttpStatus.UNAUTHORIZED, AUTHENTICATION_REQUIRED))
                    .accessDeniedHandler(
                        (_, response, _) ->
                            errors.write(response, HttpStatus.FORBIDDEN, ACCESS_DENIED)))
        .addFilterBefore(
            new JwtAuthenticationFilter(jwtService, userRepository),
            UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
