package org.tikcoin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tikcoin.model.Buyer;

import java.util.Optional;

@Repository
public interface BuyerRepository extends JpaRepository<Buyer, Long> {
    Optional<Buyer> findByEmailAddress(String emailAddress);
    Optional<Buyer> findByTiktokOpenId(String tiktokOpenId);

    boolean existsByEmailAddress(String emailAddress);
}
