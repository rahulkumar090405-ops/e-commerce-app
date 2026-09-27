package com.spring.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderResponse(

        Long orderId,

        Long customerId,

        Long itemId,

        String itemName,

        BigDecimal itemQuantity,

        BigDecimal originalPrice,

        BigDecimal discountAmount,

        BigDecimal payableAmount,

        String orderStatus,

        String paymentState,

        Date createdAt,

        Date updatedAt

) {
}
