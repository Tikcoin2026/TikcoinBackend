package org.tikcoin.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentInitResponse {
    private String authorizationUrl;
    private String accessCode;
    private String reference;
    private BigDecimal amount;
    private Long coinAmount;
    private BigDecimal coinRate;
}