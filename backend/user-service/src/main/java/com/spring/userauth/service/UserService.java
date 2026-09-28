package com.spring.userauth.service;

import java.util.List;
import java.util.Random;
import java.util.Comparator;

import org.springframework.stereotype.Service;

import com.spring.userauth.entity.User;
import com.spring.userauth.dto.ProfileUpdateRequest;
import com.spring.userauth.dto.UserProfileResponse;
import com.spring.userauth.exception.UserNotFoundException;
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
		return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found"));
	}

	public UserProfileResponse getProfileByEmail(String email) {
		return toProfileResponse(findByEmail(email));
	}

	public UserProfileResponse updateProfile(String email, ProfileUpdateRequest request) {
		User user = findByEmail(email);
		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName().trim());
		user.setGender(request.gender().trim());
		user.setMobile(request.mobile().trim());
		user.setName(user.getFirstName() + " " + user.getLastName());
		return toProfileResponse(userRepository.save(user));
	}

	public List<UserProfileResponse> getAllProfiles() {
		return userRepository.findAll().stream()
				.map(this::toProfileResponse)
				.toList();
	}

	private User findByEmail(String email) {
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("User not found"));
	}

	private UserProfileResponse toProfileResponse(User user) {
		List<String> roles = user.getRoles().stream()
				.map(role -> role.getName().name())
				.sorted(Comparator.naturalOrder())
				.toList();

		return new UserProfileResponse(
				user.getId(),
				user.getFirstName(),
				user.getLastName(),
				user.getName(),
				user.getEmail(),
				user.getGender(),
				user.getMobile(),
				roles,
				user.getCreatedDate());
	}

	
	
	
}
