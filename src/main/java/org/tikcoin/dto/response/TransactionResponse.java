package org.tikcoin.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {
    private Long id;
    private String tiktokDisplayName;
    private BigDecimal amount;
    private BigDecimal coinRate;
    private Long coinAmount;
    private String paymentStatus;
    private String tiktokAuthStatus;
    private String paystackReference;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}