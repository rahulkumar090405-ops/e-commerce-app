package com.spring.product.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.spring.product.entity.Product;
import com.spring.product.exception.BadRequestException;
import com.spring.product.repository.ProductRepository;
import com.spring.product.service.ProductService;

@RestController
@RequestMapping("/products")
public class ProductController {

	private static ProductService productService;
	private static ProductRepository productRepository;

	public ProductController(ProductService productService,ProductRepository productRepository)
	{
		this.productService = productService;
		this.productRepository = productRepository;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Product createOrder(@RequestBody Product product)
	{
		return productService.createOrder(product);
	}

	@GetMapping
	public List<Product> getProducts()
	{
		return productService.getAllProducts();
	}

	@GetMapping("/{id}")
	public Product getProduct(@PathVariable Long id)
	{
		return productService.getProduct(id);
	}

	@PutMapping("/{id}")
	public Product updateProduct(@PathVariable("id") Long id,@RequestBody Product product)
	{
		if(id != product.getId())
		{
			throw new BadRequestException("id on path doesn't match body");
		}
		return productService.updateProduct(id,product);
	}
	
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.RESET_CONTENT)
	public void deleteProduct(@PathVariable("id") Long id)
	{
		productRepository.deleteById(id);
	}
}
