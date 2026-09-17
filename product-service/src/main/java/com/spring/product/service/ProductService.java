package com.spring.product.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.spring.product.entity.Product;
import com.spring.product.exception.BadRequestException;
import com.spring.product.repository.ProductRepository;

@Service
public class ProductService {
	
	private ProductRepository productRepository;
	
	private ProductService(ProductRepository productRepository)
	{
		this.productRepository = productRepository;
	}
	
	public Product createOrder(Product product) {
		return productRepository.save(product);
	}

	public List<Product> getAllProducts() {
		return productRepository.findAll();
	}

	public Product getProduct(Long id) {
		return productRepository.findById(id).orElseThrow(() -> new BadRequestException("Product not found"));
	}

	public Product updateProduct(Long id, Product product) {
		// TODO Auto-generated method stub
		return productRepository.save(product);
	}

}
