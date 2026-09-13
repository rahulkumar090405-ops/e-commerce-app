package com.spring.userauth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.spring.userauth.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
	
	

}
