package com.spring.userauth.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spring.userauth.dto.AuthResponse;
import com.spring.userauth.dto.LoginRequest;
import com.spring.userauth.dto.RegisterRequest;
import com.spring.userauth.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	
	private final AuthService authService;
	
	public AuthController(AuthService authService)
	{
		this.authService = authService;
		
	}
	
	@PostMapping("/register")
	public String register(@Valid @RequestBody RegisterRequest request)
	{
		return authService.register(request);
	}
	
	
	@PostMapping("/login")
	public AuthResponse login(@RequestBody LoginRequest loginRequest)
	{
		return authService.login(loginRequest);
	}

	@PostMapping("/logout")
	public AuthResponse logout(@RequestBody LoginRequest loginRequest)
	{
		return authService.logout(loginRequest);
	}
	
}
