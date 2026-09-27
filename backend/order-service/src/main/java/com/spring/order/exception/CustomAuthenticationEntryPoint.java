package com.spring.order.exception;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException, ServletException {
		ErrorResponse errorResponse = new ErrorResponse(
				HttpStatus.UNAUTHORIZED.value(),
				"JWT token is expired or invalid",
				request.getRequestURI(),
				LocalDateTime.now()
				);

		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType("application/json");

		response.getWriter().write(
				objectMapper.writeValueAsString(errorResponse)
				);

	}

}
