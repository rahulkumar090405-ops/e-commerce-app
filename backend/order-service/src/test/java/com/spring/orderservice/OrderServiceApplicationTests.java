package com.spring.orderservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = com.spring.order.OrderServiceApplication.class)
@org.springframework.test.context.ActiveProfiles("test")
class OrderServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
