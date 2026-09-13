package com.spring.order.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.spring.order.entity.Order;
import com.spring.order.repository.OrderRepository;

@Service
public class OrderService {
	
	private OrderRepository orderRepository;
	
	public OrderService(OrderRepository orderRepository)
	{
		this.orderRepository = orderRepository;
	}
	
	public Order createOrder(Order order) {
		// TODO Auto-generated method stub
		return orderRepository.save(order);
	}

	public List<Order> getOrdersByUserId(Long userId) {
		// TODO Auto-generated method stub
		return orderRepository.findByUserId(userId);
	}

}
