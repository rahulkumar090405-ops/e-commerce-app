package com.spring.order.exception;

public class OrderNotFoundException extends RuntimeException{
	
	public OrderNotFoundException(String message)
	{
		super(message);
	}
}
