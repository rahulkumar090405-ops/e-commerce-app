package com.spring.order.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.spring.order.dto.OrderResponse;
import com.spring.order.entity.Order;
import com.spring.order.service.OrderService;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // USER + ADMIN + CUSTOMER
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'CUSTOMER')")
    @PostMapping("/{productId}")
    public ResponseEntity<CreateOrderResponse> createOrder(
            @PathVariable Long productId) {

        Order order = orderService.createOrder(productId);

        return ResponseEntity.ok(
                new CreateOrderResponse(
                        order.getId(),
                        order.getProductId(),
                        "Order placed successfully!"
                )
        );
    }

    // USER + ADMIN + CUSTOMER
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'CUSTOMER')")
    @GetMapping("/user/{userId}")
    public List<Order> getOrders(
            @PathVariable Long userId,Authentication authentication) {

        return orderService.getOrdersByUserId(userId,authentication);
    }

    // USER + ADMIN
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/{id}")
    public Order getOrderDetail(
            @PathVariable Long id) {

        return orderService.getOrderById(id);
    }

    // USER + ADMIN
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping
    public List<OrderResponse> getOrdersAll() {

        return orderService.getAllOrders();
    }

    // ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public Order updateOrder(
            @PathVariable Long id,
            @RequestBody Order order) {

        return orderService.updateOrder(id, order);
    }

    // ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrder(
            @PathVariable Long id) {
        orderService.deleteOrder(id);
    }
    
    @GetMapping("/debug/test-auth")
    public String testAuth(Authentication authentication) {

        System.out.println("=================================");
        System.out.println("AUTH OBJECT: " + authentication);
        System.out.println("USERNAME: " + authentication.getName());
        System.out.println("AUTHORITIES: " + authentication.getAuthorities());
        System.out.println("=================================");

        return "Authentication working";
    }
}