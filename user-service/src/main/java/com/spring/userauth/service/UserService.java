package com.spring.userauth.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.spring.userauth.entity.User;
import com.spring.userauth.repository.UserRepository;

@Service
public class UserService {
	
	private UserRepository userRepository;
	private Random random = new Random();
	
	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}
	
	public User createRandomUser() {
		
		String name = "User"+random.nextInt(100000);
		String email = "user"+random.nextInt(100000)+"@gmail.com";
		Integer age = random.nextInt(43) + 18;
		
		User user = new User(name, email, age);
		
		return userRepository.save(user);
	}

	public List<User> getAllUser() {
		// TODO Auto-generated method stub
		return userRepository.findAll();
	}

	public Long getUserCount() {
		// TODO Auto-generated method stub
		return userRepository.count();
	}

	public User createUser(User user) {
		// TODO Auto-generated method stub
		return userRepository.save(user);
	}

	public User findById(Long id) {
		// TODO Auto-generated method stub
		return userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
	}

	
	
	
}
