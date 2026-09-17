package com.spring.order.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.spring.order.dto.ProductResponse;

@FeignClient(name="product-service",url="http://localhost:8082")
public interface ProductClient {

	@GetMapping("/products/{id}")
	public ProductResponse getProductById(@PathVariable("id") Long id);
}
