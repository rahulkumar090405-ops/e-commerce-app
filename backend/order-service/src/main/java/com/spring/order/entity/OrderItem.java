package com.spring.order.entity;

import java.io.Serializable;
import java.math.BigDecimal;

import com.spring.order.utils.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="order_item")
public class OrderItem  extends BaseEntity implements Serializable{

	private Long orderId;
	private BigDecimal basePrice;
	private BigDecimal discount;
	private BigDecimal finalPrice;

}
