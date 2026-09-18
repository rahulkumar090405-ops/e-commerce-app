package com.spring.userauth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.spring.userauth.dto.LoginRequest;
import com.spring.userauth.dto.RegisterRequest;
import com.spring.userauth.entity.User;
import com.spring.userauth.repository.UserRepository;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	
	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager)
	{
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
	}
	
	public String register(RegisterRequest request)
	{
		if (userRepository.findByEmail(request.email()).isPresent()) {
		    return "Email already registered";
		}
		User user = new User();
		user.setName(request.name());
		user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole("USER");
		userRepository.save(user);
		return "User registered successfully.";
	}
	
	public String login(LoginRequest request)
	{
		Authentication authentication =
                authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                    )
                );

        return "Login successful";
	}
	
}
