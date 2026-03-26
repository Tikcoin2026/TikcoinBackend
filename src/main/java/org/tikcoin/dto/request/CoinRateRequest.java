package org.tikcoin.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CoinRateRequest {

    // coin unit reference, e.g. 1 (meaning "1 coin")
    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0.01", message = "Rate must be greater than zero")
    private BigDecimal rate;

    // naira value for that coin unit, e.g. 25 (meaning "₦25 per 1 coin")
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;
}