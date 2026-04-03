package org.tikcoin.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.tikcoin.model.Admin;
import org.tikcoin.repository.AdminRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FirebaseNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseNotificationService.class);

    private final AdminRepository adminRepository;

    /**
     * Sends a push notification to all admins that have registered an FCM token.
     */
    public void notifyAdminsOfNewPayment(String buyerDisplayName, BigDecimal amount, Long coinAmount) {
        if (FirebaseApp.getApps().isEmpty()) {
            logger.warn("Firebase not initialized — skipping push notification");
            return;
        }

        List<Admin> admins = adminRepository.findAllWithFcmToken();
        if (admins.isEmpty()) {
            logger.info("No admin FCM tokens registered — skipping push notification");
            return;
        }

        String title = "New Payment Received";
        String body = String.format("%s just paid ₦%s for %d TikCoin(s)!",
                buyerDisplayName != null ? buyerDisplayName : "A user",
                amount.toPlainString(),
                coinAmount);

        for (Admin admin : admins) {
            sendToDevice(admin.getFcmToken(), title, body);
        }
    }

    private void sendToDevice(String fcmToken, String title, String body) {
        if (fcmToken == null || fcmToken.isBlank()) return;

        Message message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setToken(fcmToken)
                .build();

        try {
            String messageId = FirebaseMessaging.getInstance().send(message);
            logger.info("FCM notification sent: {}", messageId);
        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send FCM notification to token {}: {}", fcmToken, e.getMessage());
        }
    }
}