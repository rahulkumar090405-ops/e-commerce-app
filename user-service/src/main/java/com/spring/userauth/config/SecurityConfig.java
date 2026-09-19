package com.spring.userauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.spring.userauth.security.JwtAuthenticationFilter;


@Configuration
@EnableWebSecurity
public class SecurityConfig {
 
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	public SecurityConfig(
	        JwtAuthenticationFilter jwtAuthenticationFilter) {

	    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception
	{
		http
	    .csrf(csrf -> csrf.disable())
	    .sessionManagement(session ->
	        session.sessionCreationPolicy(
	            SessionCreationPolicy.STATELESS
	        )
	    )
	    .authorizeHttpRequests(auth -> auth
	        .requestMatchers(
	            "/auth/register",
	            "/auth/login"
	        ).permitAll()
	        .anyRequest().authenticated()
	    )
	    .addFilterBefore(
	        jwtAuthenticationFilter,
	        UsernamePasswordAuthenticationFilter.class
	    );
		
		return http.build();
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
