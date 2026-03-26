package org.tikcoin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tikcoin.dto.response.NotificationResponse;
import org.tikcoin.model.Notification;
import org.tikcoin.repository.NotificationRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Notification createRateChangeNotification(BigDecimal nairaPerCoin, BigDecimal rate) {
        String title = String.format(
                "Coin rates has been updated to %s naira for %s coin",
                nairaPerCoin.stripTrailingZeros().toPlainString(),
                rate.stripTrailingZeros().toPlainString()
        );
        Notification notification = Notification.builder()
                .title(title)
                .timestamp(LocalDateTime.now())
                .build();
        return notificationRepository.save(notification);
    }

    public List<NotificationResponse> getAllNotifications() {
        return notificationRepository.findAllByOrderByTimestampDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .timestamp(notification.getTimestamp())
                .build();
    }
}