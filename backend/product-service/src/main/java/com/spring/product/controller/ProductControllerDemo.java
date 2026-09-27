package com.spring.product.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestParam;
import com.spring.product.entity.Product;
import com.spring.product.repository.ProductRepository;
import com.spring.product.service.ProductService;

@Controller
@RequestMapping("demo/products")
public class ProductControllerDemo {

	private static ProductRepository productRepository;
	
	public ProductControllerDemo(ProductRepository productRepository)
	{
		this.productRepository = productRepository;
	}
	
	@PostMapping
	public Product createOrder(@RequestBody Product product)
	{
		return productRepository.save(product);
	}
	
	@GetMapping
	public String getProducts(@RequestParam(defaultValue = "0") int page,@RequestParam(defaultValue = "8") int size,Model model)
	{
		Pageable pageable = PageRequest.of(page, size);
		Page<Product> productPage = productRepository.findAll(pageable);
			
		model.addAttribute("products",productPage.getContent());
		model.addAttribute("currentPage", page);
	    model.addAttribute("totalPages", productPage.getTotalPages());
	    model.addAttribute("totalItems", productPage.getTotalElements());
	    model.addAttribute("pageSize", size);

		return "products";
	}
	
	@GetMapping("/{id}")
	public String getProduct(
	        @PathVariable Long id,
	        Model model) {

	    Product product = productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));

	    model.addAttribute("product", product);

	    return "product-detail";
	}
}
