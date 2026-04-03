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

    public void notifyAdminsOfNewPayment(String buyerDisplayName, BigDecimal amount, Long coinAmount) {
        if (FirebaseApp.getApps().isEmpty()) {
            logger.error("Firebase is NOT initialized — FIREBASE_SERVICE_ACCOUNT_BASE64 may not be set on the server");
            return;
        }

        List<Admin> admins = adminRepository.findAllWithFcmToken();
        logger.info("Found {} admin(s) with FCM tokens", admins.size());

        if (admins.isEmpty()) {
            logger.warn("No admin FCM tokens registered — call POST /api/admin/fcm-token first");
            return;
        }

        String title = "New Payment Received";
        String body = String.format("%s just paid \u20a6%s for %d TikCoin(s)!",
                buyerDisplayName != null ? buyerDisplayName : "A user",
                amount.toPlainString(),
                coinAmount);

        logger.info("Sending FCM notification: title='{}', body='{}'", title, body);

        for (Admin admin : admins) {
            sendToDevice(admin.getFcmToken(), title, body);
        }
    }

    private void sendToDevice(String fcmToken, String title, String body) {
        if (fcmToken == null || fcmToken.isBlank()) {
            logger.warn("Skipping FCM — token is null or blank");
            return;
        }
        logger.info("Sending FCM to token ending in ...{}", fcmToken.substring(Math.max(0, fcmToken.length() - 10)));
        Message message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setToken(fcmToken)
                .build();

        try {
            String messageId = FirebaseMessaging.getInstance().send(message);
            logger.info("FCM notification sent successfully, messageId: {}", messageId);
        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send FCM notification: {} — errorCode: {}", e.getMessage(), e.getMessagingErrorCode());
        }
    }
}