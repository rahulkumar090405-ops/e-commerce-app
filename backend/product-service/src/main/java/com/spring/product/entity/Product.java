package com.spring.product.entity;

import java.io.Serializable;
import java.math.BigDecimal;

import com.spring.product.utils.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name="products")
public class Product extends BaseEntity implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String category;
	private String productName;
	private String description;
	private BigDecimal price;
	private BigDecimal stock;
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public BigDecimal getPrice() {
		return price;
	}
	public void setPrice(BigDecimal price) {
		this.price = price;
	}
	public String getProductName() {
		return productName;
	}
	public void setProductName(String productName) {
		this.productName = productName;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public BigDecimal getStock() {
		return stock;
	}
	public void setStock(BigDecimal stock) {
		this.stock = stock;
	}
	
}
