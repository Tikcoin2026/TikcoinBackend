package org.tikcoin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tikcoin.model.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}