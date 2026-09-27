package com.spring.userauth.dto;

public record LoginResponse(
		String username,
		String email,
		String password) {

}
