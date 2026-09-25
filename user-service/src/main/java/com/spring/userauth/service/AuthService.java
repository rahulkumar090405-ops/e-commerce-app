package com.spring.userauth.service;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.spring.userauth.dto.AuthResponse;
import com.spring.userauth.dto.LoginRequest;
import com.spring.userauth.dto.RegisterRequest;
import com.spring.userauth.entity.Role;
import com.spring.userauth.entity.User;
import com.spring.userauth.enumer.RoleName;
import com.spring.userauth.repository.RoleRepository;
import com.spring.userauth.repository.UserRepository;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	
	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService, RoleRepository roleRepository)
	{
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
	}
	
	public String register(RegisterRequest request)
	{
		if (userRepository.findByEmail(request.email()).isPresent()) {
		    return "Email already registered";
		}
		User user = new User();
		user.setCreatedDate(new Date());
		user.setUpdatedDate(new Date());
		user.setName(request.name());
		user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        
        Set<Role> roles = new HashSet<>();
        for (RoleName roleName : request.roles()) {

            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() ->
                            new RuntimeException("Role not found: " + roleName));

            roles.add(role);
        }
        user.setRoles(roles);
		userRepository.save(user);
		return "User registered successfully.";
	}
	
	public AuthResponse login(LoginRequest request)
	{
		Authentication authentication =
                authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                    )
                );

		User user = userRepository.findByEmail(request.email())
		        .orElseThrow(() -> new UsernameNotFoundException("User not found"));

	    String token =
	            jwtService.generateToken(user);

	    return new AuthResponse(token);

	}
	
}
