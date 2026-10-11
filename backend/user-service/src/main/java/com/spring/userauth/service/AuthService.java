package com.spring.userauth.service;

import java.util.Date;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.spring.userauth.dto.AuthResponse;
import com.spring.userauth.dto.LoginRequest;
import com.spring.userauth.dto.RegisterRequest;
import com.spring.userauth.entity.Role;
import com.spring.userauth.entity.User;
import com.spring.userauth.enumer.RoleName;
import com.spring.userauth.exception.EmailAlreadyExistsException;
import com.spring.userauth.exception.InvalidPasswordException;
import com.spring.userauth.exception.UserNotFoundException;
import com.spring.userauth.repository.RoleRepository;
import com.spring.userauth.repository.UserRepository;

@Service
public class AuthService {

	private static final Set<RoleName> ADMIN_WORKSPACE_ROLES = Set.of(
			RoleName.ROLE_ADMIN);

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
		    throw new EmailAlreadyExistsException("Email already registered");
		}

		RoleName roleName = registrationRole(request.registrationType());
		Role registrationRole = roleRepository.findByName(roleName)
				.orElseThrow(() -> new RuntimeException("Registration role not found: " + roleName));

		User user = new User();
		user.setCreatedDate(new Date());
		user.setUpdatedDate(new Date());
		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName().trim());
		user.setGender(request.gender().trim());
		user.setMobile(request.mobile().trim());
		user.setName(user.getFirstName() + " " + user.getLastName());
		user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
		user.setRoles(Set.of(registrationRole));
		userRepository.save(user);
		return "User registered successfully.";
	}

	private RoleName registrationRole(String registrationType) {
		if (registrationType == null || registrationType.isBlank()
				|| "CUSTOMER".equalsIgnoreCase(registrationType)) {
			return RoleName.ROLE_CUSTOMER;
		}

		if ("USER".equalsIgnoreCase(registrationType)) {
			return RoleName.ROLE_USER;
		}

		throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Registration type must be CUSTOMER or USER");
	}
	
	public AuthResponse login(LoginRequest request) {
		Set<RoleName> allowedRoles = "ADMIN".equalsIgnoreCase(request.loginType())
				? ADMIN_WORKSPACE_ROLES
				: Set.of();
		return authenticate(request, allowedRoles);
	}

	private AuthResponse authenticate(LoginRequest request, Set<RoleName> allowedRoles) {

	    User user = userRepository.findByEmail(request.email())
	        .orElseThrow(() ->
	            new UserNotFoundException("User not found"));

	    if (!user.isEnabled()) {
	        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled");
	    }

	    try {
	        authenticationManager.authenticate(
	            new UsernamePasswordAuthenticationToken(
	                request.email(),
	                request.password()
	            )
	        );
	    } catch (BadCredentialsException e) {
	        throw new InvalidPasswordException("Invalid password");
	    }

	    if (!allowedRoles.isEmpty() && user.getRoles().stream()
	    			.noneMatch(role -> allowedRoles.contains(role.getName()))) {
	    	throw new ResponseStatusException(
	    			HttpStatus.FORBIDDEN,
	    			"Administrator access is required");
	    }

	    String token = jwtService.generateToken(user);

	    return new AuthResponse(token);
	}

	
}
