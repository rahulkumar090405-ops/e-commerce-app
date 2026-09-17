package com.spring.order.dto;

public record OrderResponse(
		 Long orderId,
		 Long productId,
		 String message
		)
{
}
