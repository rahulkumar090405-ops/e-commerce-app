package com.spring.userauth.exception;

import java.time.LocalDateTime;

import javax.management.relation.RoleNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ErrorResponse> handleResponseStatusException(
			ResponseStatusException ex,
			HttpServletRequest request) {
		HttpStatusCode status = ex.getStatusCode();
		String message = ex.getReason() == null ? "Request was rejected" : ex.getReason();
		ErrorResponse errorResponse = new ErrorResponse(
				status.value(),
				message,
				request.getRequestURI(),
				LocalDateTime.now());

		return ResponseEntity.status(status).body(errorResponse);
	}
	
	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex, HttpServletRequest request)
	{
		ErrorResponse errorResponse = new ErrorResponse(
				HttpStatus.NOT_FOUND.value(),
				ex.getMessage(),
				request.getRequestURI(),
				LocalDateTime.now());
		
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
	}
	@ExceptionHandler(ResourceAccessException.class)
	public ResponseEntity<ErrorResponse> handleResourceAccessException(ResourceAccessException ex,HttpServletRequest request)
	{
		 System.out.println(
	                "========== GLOBAL EXCEPTION HANDLER =========="
	        );

	        System.out.println(
	                "Exception: " + ex.getClass().getName()
	        );
		ErrorResponse errorResponse = new ErrorResponse(
				HttpStatus.SERVICE_UNAVAILABLE.value(),
				"Order service is currently unavailable",
				request.getRequestURI(),
				LocalDateTime.now());
		
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
		
	}
	
	@ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<String> handleInvalidPassword(
            InvalidPasswordException ex) {

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ex.getMessage());
    }
	
	@ExceptionHandler(EmailAlreadyExistsException.class)
	public ResponseEntity<String> handleEmailAlreadyExists(
	        EmailAlreadyExistsException ex) {

	    return ResponseEntity
	            .status(HttpStatus.CONFLICT)
	            .body(ex.getMessage());
	}
	@ExceptionHandler(RoleNotFoundException.class)
	public ResponseEntity<String> handleRoleNotFound(
	        RoleNotFoundException ex) {

	    return ResponseEntity
	            .status(HttpStatus.NOT_FOUND)
	            .body(ex.getMessage());
	}
}
