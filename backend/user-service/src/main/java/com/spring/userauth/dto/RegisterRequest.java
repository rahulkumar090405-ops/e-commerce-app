package com.spring.userauth.dto;

import java.util.Set;

import com.spring.userauth.enumer.RoleName;

public record RegisterRequest(String name,String email,String password,String token,Set<RoleName> roles) {

}
