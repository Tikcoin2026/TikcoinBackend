package org.tikcoin.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CoinRateResponse {
    private Long id;
    private BigDecimal rate;
    private BigDecimal amount;
    private BigDecimal nairaPerCoin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}