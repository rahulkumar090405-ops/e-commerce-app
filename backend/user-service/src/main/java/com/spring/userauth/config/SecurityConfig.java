package com.spring.userauth.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.spring.userauth.security.CustomAccessDeniedHandler;
import com.spring.userauth.security.JwtAuthenticationFilter;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final CustomAccessDeniedHandler customAccessDeniedHandler;
	public SecurityConfig(
			JwtAuthenticationFilter jwtAuthenticationFilter, CustomAccessDeniedHandler customAccessDeniedHandler) {

		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.customAccessDeniedHandler = customAccessDeniedHandler;
	}
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception
	{
		http
		.csrf(csrf -> csrf.disable())
		.cors(cors ->{})
		.sessionManagement(session ->
		session.sessionCreationPolicy(
				SessionCreationPolicy.STATELESS
				)
				)
		.exceptionHandling(exception ->
        exception.accessDeniedHandler(
            customAccessDeniedHandler
        )
	    )
		.authorizeHttpRequests(auth -> auth
				.requestMatchers(
						"/api/auth/register",
						"/api/auth/login"
						).permitAll()

				// Admin only
				.requestMatchers("/api/admin/**")
				.hasAnyRole("ADMIN", "USER", "EMPLOYEE")

				// Employee only
				.requestMatchers("/api/employee/**")
				.hasRole("EMPLOYEE")

				// Customer only
				.requestMatchers("/api/customer/**")
				.hasRole("CUSTOMER")

				.anyRequest().authenticated()
				)
		.addFilterBefore(
				jwtAuthenticationFilter,
				UsernamePasswordAuthenticationFilter.class
				);

		return http.build();
	}
	
	@Bean
	CorsConfigurationSource  corsConfigurationSource()
	{
		CorsConfiguration  configuration = new  CorsConfiguration ();
		
		configuration.setAllowedOrigins(List.of("http://localhost:5174","http://localhost:5173"));
		
		configuration.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
		
		configuration.setAllowedHeaders(List.of("Authorization",
		        "Content-Type"));
		
		configuration.setAllowCredentials(true);
		
		 UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

		 source.registerCorsConfiguration(
			        "/**",
			        configuration
			    );
		
		return source;
		
	}

	@Bean
	PasswordEncoder passwordEncoder()
	{
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(
			UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {

		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);

		provider.setPasswordEncoder(passwordEncoder);

		return new ProviderManager(provider);
	}

}
