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

import com.spring.product.entity.Category;
import com.spring.product.exception.BadRequestException;
import com.spring.product.exception.NotFoundException;
import com.spring.product.repository.CategoryRepository;

@RestController
@RequestMapping("/api/category")
public class CategoryController {

	private static CategoryRepository categoryRepository;

	public CategoryController(CategoryRepository CategoryRepository)
	{
		this.categoryRepository = CategoryRepository;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Category create(@RequestBody Category Category)
	{
		return categoryRepository.save(Category);
	}

	@GetMapping
	public List<Category> getCategorys()
	{
		return categoryRepository.findAll();
	}

	@GetMapping("/{id}")
	public Category getCategory(@PathVariable Long id)
	{
		return categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("category not found"));
	}

	@PutMapping("/{id}")
	public Category updateCategory(@PathVariable("id") Long id,@RequestBody Category category)
	{
		if (!id.equals(category.getId())) {
	        throw new BadRequestException("ID on path doesn't match body");
	    }
		return categoryRepository.save(category);
	}
	
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.RESET_CONTENT)
	public void deleteCategory(@PathVariable("id") Long id)
	{
		categoryRepository.deleteById(id);
	}
}
