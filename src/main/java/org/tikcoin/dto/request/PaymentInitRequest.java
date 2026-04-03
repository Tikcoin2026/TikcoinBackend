package org.tikcoin.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentInitRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    private String callbackUrl;
}