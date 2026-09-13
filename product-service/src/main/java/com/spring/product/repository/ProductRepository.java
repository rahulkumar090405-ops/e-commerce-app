package com.spring.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.spring.product.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

}
