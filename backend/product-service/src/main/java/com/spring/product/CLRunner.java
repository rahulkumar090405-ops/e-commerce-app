package com.spring.product;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.spring.product.entity.Product;
import com.spring.product.repository.ProductRepository;

@Component
public class CLRunner implements CommandLineRunner {
	
	private final ProductRepository productRepository;
	
	public CLRunner(ProductRepository productRepository)
	{
		this.productRepository = productRepository;
	}
	
	@Override
	public void run(String... args) throws Exception {
		/*
		 * List<Product> products = this.productRepository.findAll(); Optional<Product>
		 * product = this.productRepository.findByProductNameIgnoreCase("iphone");
		 * System.out.println(product); products.forEach(System.out::println);
		 */
	}

}
