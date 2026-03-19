package org.tikcoin.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderRequest {
    @NotNull(message = "Rate is required")
    @Min(value = 1, message = "Rate must be greater than zero")
    private BigDecimal rate;

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be at least 1")
    private Long amount;
}