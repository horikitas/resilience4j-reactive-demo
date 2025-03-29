package org.horikita.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class OrderRequestDTO {
    private int orderId;
    private String productId;
}
