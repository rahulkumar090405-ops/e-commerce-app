package com.spring.product.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.spring.product.entity.Category;
import com.spring.product.entity.Product;

public interface CategoryRepository extends JpaRepository<Category, Long> {
	
	Optional<Category> findByCategoryNameIgnoreCase(String categoryName);
	
}
