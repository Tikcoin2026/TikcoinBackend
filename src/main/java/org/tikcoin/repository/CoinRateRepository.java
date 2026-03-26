package org.tikcoin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tikcoin.model.CoinRate;

import java.util.Optional;

public interface CoinRateRepository extends JpaRepository<CoinRate, Long> {
    Optional<CoinRate> findTopByOrderByCreatedAtDesc();
}