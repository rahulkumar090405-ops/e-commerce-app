package com.spring.order.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spring.order.entity.Order;
import com.spring.order.service.OrderService;

@RestController
@RequestMapping("/orders")
public class OrderController {
	
	private final OrderService orderService;
	
	public OrderController(OrderService orderService)
	{
		this.orderService = orderService;
	}
	
	@PostMapping
	public Order createOrder(@RequestBody Order order)
	{
		return orderService.createOrder(order);
		
	}
	
	@GetMapping("/{userId}")
	public List<Order> getOrders(@PathVariable Long userId)
	{
		return orderService.getOrdersByUserId(userId);
		
	}
	
	
	
}
