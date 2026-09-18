package com.spring.userauth.dto;

public record LoginRequest(
		String email,
		String password) {

}
