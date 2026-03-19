package org.tikcoin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tikcoin.model.Admin;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByEmailAddress(String emailAddress);
    Optional<Admin> findByTiktokOpenId(String tiktokOpenId);

    boolean existsByEmailAddress(String emailAddress);
}
