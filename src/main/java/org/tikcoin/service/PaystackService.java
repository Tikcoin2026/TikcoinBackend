package org.tikcoin.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.tikcoin.exception.BadRequestException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaystackService {

    private static final Logger logger = LoggerFactory.getLogger(PaystackService.class);
    private static final String PAYSTACK_BASE_URL = "https://api.paystack.co";

    @Value("${paystack.secret-key}")
    private String secretKey;

    private final RestTemplate restTemplate;

    public Map<String, Object> initializeTransaction(String email, BigDecimal amountNaira,
                                                      String reference, String callbackUrl) {
        long amountKobo = amountNaira.multiply(BigDecimal.valueOf(100)).longValue();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(secretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("amount", amountKobo);
        body.put("reference", reference);
        if (callbackUrl != null && !callbackUrl.isBlank()) {
            body.put("callback_url", callbackUrl);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    PAYSTACK_BASE_URL + "/transaction/initialize", entity, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !Boolean.TRUE.equals(responseBody.get("status"))) {
                String msg = responseBody != null ? String.valueOf(responseBody.get("message")) : "null response";
                logger.error("Paystack initialization failed: {}", msg);
                throw new BadRequestException("Payment initialization failed: " + msg);
            }

            return (Map<String, Object>) responseBody.get("data");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Paystack initialization error", e);
            throw new BadRequestException("Payment initialization failed: " + e.getMessage());
        }
    }

    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec keySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = bytesToHex(hash);
            return computed.equals(signature);
        } catch (Exception e) {
            logger.error("Webhook signature verification error", e);
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}