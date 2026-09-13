package com.spring.product.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spring.product.entity.Product;
import com.spring.product.service.ProductService;

@RestController
@RequestMapping("/products")
public class ProductController {

	private static ProductService productService;
	
	public ProductController(ProductService productService)
	{
		this.productService = productService;
	}
	
	@PostMapping
	public Product createOrder(@RequestBody Product product)
	{
		return productService.createOrder(product);
	}
	
	@GetMapping
	public List<Product> getOrder()
	{
		return productService.getOrders();
	}
	
	@GetMapping("/{id}")
	public Product getOrderDetail(@PathVariable Long id)
	{
		return productService.getOrderDetail(id);
		
	}
}
