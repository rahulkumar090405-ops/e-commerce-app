package com.spring.order.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final CustomAccessDeniedHandler accessDeniedHandler;
	
	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, CustomAccessDeniedHandler accessDeniedHandler)
	{
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.accessDeniedHandler = accessDeniedHandler;
		
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http)
			throws Exception {

		http
		.csrf(csrf -> csrf.disable())
		.sessionManagement(session ->
		session.sessionCreationPolicy(
				SessionCreationPolicy.STATELESS
				)
				)
		.exceptionHandling(exception ->
        exception.accessDeniedHandler(accessDeniedHandler)
)
		.authorizeHttpRequests(auth -> auth
				.requestMatchers("/swagger-ui/**").permitAll()
				.requestMatchers("/v3/api-docs/**").permitAll()
				 .requestMatchers("/actuator/health").permitAll()
				 .requestMatchers("/debug/test-auth").permitAll()
				.requestMatchers("/orders/**")
				.authenticated()

				.anyRequest()
				.authenticated()
				)
		.addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );	
		return http.build();
	}

	/*
	 * @Bean public JwtDecoder jwtDecoder(
	 * 
	 * @Value("${jwt.secret}") String secret) {
	 * 
	 * SecretKey key = new SecretKeySpec( secret.getBytes(StandardCharsets.UTF_8),
	 * "HmacSHA384" );
	 * 
	 * return NimbusJwtDecoder .withSecretKey(key) .macAlgorithm(MacAlgorithm.HS384)
	 * .build(); }
	 */

}
