package org.tikcoin.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.util.Base64;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.service-account-base64:}")
    private String serviceAccountBase64;

    @PostConstruct
    public void initialize() {
        if (serviceAccountBase64 == null || serviceAccountBase64.isBlank()) {
            logger.warn("Firebase service account not configured — push notifications disabled");
            return;
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(serviceAccountBase64);
            ByteArrayInputStream stream = new ByteArrayInputStream(decoded);

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(stream))
                    .build();

            FirebaseApp.initializeApp(options);
            logger.info("Firebase initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Firebase: {}", e.getMessage(), e);
        }
    }
}