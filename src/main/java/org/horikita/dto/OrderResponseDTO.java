package org.horikita.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class OrderResponseDTO {
    private int orderId;
    private OrderStatusEnum status;
}
