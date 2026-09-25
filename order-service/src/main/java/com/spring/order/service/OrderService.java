package com.spring.order.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.spring.order.dto.OrderResponse;
import com.spring.order.dto.ProductResponse;
import com.spring.order.entity.Order;
import com.spring.order.exception.OrderNotFoundException;
import com.spring.order.repository.OrderRepository;
import com.spring.order.service.client.ProductClient;

@Service
public class OrderService {

	private final OrderRepository orderRepository;
	private final ProductClient productClient;

	public OrderService(OrderRepository orderRepository, ProductClient productClient)
	{
		this.orderRepository = orderRepository;
		this.productClient = productClient;
	}

	public Order createOrder(Long productId) {
		Order order =  new Order();
		if(productId != null)
		{
			ProductResponse productResponse = productClient.getProductById(productId);
			if(productResponse != null)
			{
				System.out.println("Product Service Called");
				if(productResponse.stock() != null)
				{
					if (productResponse.stock().compareTo(order.getQuantity()) < 0) {

						throw new RuntimeException(
								"Insufficient product stock"
								);
					}
				}
				order.setBasePrice(productResponse.price().multiply(order.getQuantity()!= null?order.getQuantity():new BigDecimal(1)));
				order.setProductId(productResponse.id());
				order.setBasePrice(productResponse.price());
				order.setStatus("SUCCESS");
			}
			order.setCreatedDate(new Date());
		}
		return orderRepository.save(order);
	}

	public List<Order> getOrdersByUserId(Long userId, Authentication authentication) {
		// TODO Auto-generated method stub

		return orderRepository.findByUserId(userId);
	}

	public List<OrderResponse> getAllOrders() {

	    return orderRepository.findAll()
	            .stream()
	            .map(order -> new OrderResponse(
	                    order.getId(),
	                    order.getUserId(),
	                    order.getProductId(),
	                    order.getProductName(),
	                    order.getQuantity(),
	                    order.getBasePrice(),
	                    order.getDiscount(),
	                    order.getFinalPrice(),
	                    order.getStatus(),
	                    order.getPaymentStatus(),
	                    order.getCreatedDate(),
	                    order.getUpdatedDate()

	            ))
	            .toList();
	}

	public Order getOrderById(Long id) {
		// TODO Auto-generated method stub
		return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException("Order not found."));
	}

	public void deleteOrder(Long id) {
		// TODO Auto-generated method stub
		 orderRepository.deleteById(id);
	}

	public Order updateOrder(Long id, Order updatedOrder) {
		Order existingOrder = orderRepository.findById(id)
	            .orElseThrow(() ->
	                    new RuntimeException("Order not found: " + id));

	    existingOrder.setStatus(updatedOrder.getStatus());
	    existingOrder.setBasePrice(updatedOrder.getBasePrice());
	    existingOrder.setFinalPrice(updatedOrder.getFinalPrice());
	    return orderRepository.save(existingOrder);
	}

}
