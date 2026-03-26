package org.tikcoin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tikcoin.model.Notification;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByOrderByTimestampDesc();
}