package org.tikcoin.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coin_rates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private BigDecimal nairaPerCoin;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}