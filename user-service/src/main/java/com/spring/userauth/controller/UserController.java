package com.spring.userauth.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.spring.userauth.dto.OrderDto;
import com.spring.userauth.entity.User;
import com.spring.userauth.service.UserService;

@RestController
@RequestMapping("/users")
public class UserController {

	private UserService userService;
	private RestTemplate restTemplate = new RestTemplate();

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/me")
	public String me(Authentication authentication)
	{
        return "Logged in as: "
                + authentication.getName();
	}

	@PostMapping
	public User createUser(@RequestBody User user)
	{
		return userService.createUser(user);
	}

	@GetMapping("/{id}")
	public User getUser(@PathVariable Long id)
	{
		var user = userService.findById(id);
		var url = "http://order-service:8083/orders/"+id;
		var orders = restTemplate.getForObject(url, OrderDto[].class);
		if(orders != null)
		{
			user.setOrders(List.of(orders));
		}
		return user;
	}

	@GetMapping
	public List<User> getAllUser()
	{
		return userService.getAllUser();
	}

	@GetMapping("/count")
	public Map<String,Long> getUserCount()
	{
		return Map.of("totalUsers",userService.getUserCount());

	}
}
