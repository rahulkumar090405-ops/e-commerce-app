package com.spring.order.dto;

import java.math.BigDecimal;

public record ProductResponse(
		 Long id,
		 String productName,
		 BigDecimal price,
		 BigDecimal stock
		)
{
}
