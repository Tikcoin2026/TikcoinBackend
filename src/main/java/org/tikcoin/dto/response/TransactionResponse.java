package org.tikcoin.dto.response;

import lombok.Builder;
import lombok.Data;
import org.tikcoin.enums.OrderStatus;

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
    private OrderStatus orderStatus;
    private String orderStatusLabel;
    private String tiktokAuthStatus;
    private String paystackReference;
    private String qrCode;
    private LocalDateTime qrUploadedAt;
    private LocalDateTime completedAt;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}