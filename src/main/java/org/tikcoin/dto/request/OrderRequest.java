package org.tikcoin.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderRequest {

    // Number of coins, e.g. 500 for a 500-coin order.
    // The naira total is calculated automatically using the current coin rate.
    @NotNull(message = "Coin amount is required")
    @Min(value = 1, message = "Coin amount must be at least 1")
    private Long coinAmount;
}