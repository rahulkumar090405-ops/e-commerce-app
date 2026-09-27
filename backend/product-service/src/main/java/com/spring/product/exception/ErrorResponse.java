package com.spring.product.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
		int status,
		String message,
		String path,
		LocalDateTime timestamp
		) {

}
