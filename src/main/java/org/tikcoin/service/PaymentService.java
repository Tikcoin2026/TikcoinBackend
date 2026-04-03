package org.tikcoin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tikcoin.dto.request.PaymentInitRequest;
import org.tikcoin.dto.response.PaymentInitResponse;
import org.tikcoin.dto.response.TransactionResponse;
import org.tikcoin.enums.PaymentStatus;
import org.tikcoin.exception.BadRequestException;
import org.tikcoin.exception.ResourceNotFoundException;
import org.tikcoin.model.Buyer;
import org.tikcoin.model.Order;
import org.tikcoin.model.Payment;
import org.tikcoin.repository.BuyerRepository;
import org.tikcoin.repository.OrderRepository;
import org.tikcoin.repository.PaymentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final BuyerRepository buyerRepository;
    private final PaystackService paystackService;
    private final FirebaseNotificationService firebaseNotificationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentInitResponse initializePayment(PaymentInitRequest request, String tiktokOpenId) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        Buyer buyer = buyerRepository.findByTiktokOpenId(tiktokOpenId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));

        String reference = UUID.randomUUID().toString().replace("-", "");
        String email = tiktokOpenId + "@tikcoin.app";

        Map<String, Object> paystackData = paystackService.initializeTransaction(
                email, order.getTotalAmount(), reference, request.getCallbackUrl());

        Payment payment = Payment.builder()
                .order(order)
                .buyer(buyer)
                .amount(order.getTotalAmount())
                .coinAmount(order.getAmount())
                .coinRate(order.getRate())
                .paystackReference(reference)
                .paymentStatus(PaymentStatus.PENDING)
                .tiktokAuthenticated(true)
                .createdAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        return PaymentInitResponse.builder()
                .authorizationUrl((String) paystackData.get("authorization_url"))
                .accessCode((String) paystackData.get("access_code"))
                .reference(reference)
                .amount(order.getTotalAmount())
                .coinAmount(order.getAmount())
                .coinRate(order.getRate())
                .build();
    }

    @Transactional
    public void handleWebhook(String payload, String signature) {
        if (!paystackService.verifyWebhookSignature(payload, signature)) {
            logger.warn("Invalid Paystack webhook signature — ignoring");
            throw new BadRequestException("Invalid webhook signature");
        }

        try {
            Map<String, Object> event = objectMapper.readValue(
                    payload, new TypeReference<Map<String, Object>>() {});

            String eventType = (String) event.get("event");
            if (!"charge.success".equals(eventType)) {
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            String reference = (String) data.get("reference");

            Payment payment = paymentRepository.findByPaystackReference(reference)
                    .orElse(null);

            if (payment == null) {
                logger.warn("Received webhook for unknown reference: {}", reference);
                return;
            }

            if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
                return; // already processed
            }

            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);

            logger.info("Payment {} marked as successful", reference);

            String displayName = payment.getBuyer() != null
                    ? payment.getBuyer().getTiktokUsername()
                    : "Unknown";

            firebaseNotificationService.notifyAdminsOfNewPayment(
                    displayName, payment.getAmount(), payment.getCoinAmount());

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error processing Paystack webhook", e);
            throw new BadRequestException("Webhook processing failed: " + e.getMessage());
        }
    }

    @Transactional
    public TransactionResponse verifyPayment(String reference) {
        Payment payment = paymentRepository.findByPaystackReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for reference: " + reference));

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return toTransactionResponse(payment);
        }

        Map<String, Object> data = paystackService.verifyTransaction(reference);
        String status = (String) data.get("status");

        if ("success".equalsIgnoreCase(status)) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);

            logger.info("Payment {} manually verified and marked as successful", reference);

            String displayName = payment.getBuyer() != null
                    ? payment.getBuyer().getTiktokUsername() : "Unknown";
            firebaseNotificationService.notifyAdminsOfNewPayment(
                    displayName, payment.getAmount(), payment.getCoinAmount());
        } else if ("failed".equalsIgnoreCase(status)) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
        }

        return toTransactionResponse(payment);
    }

    public List<TransactionResponse> getAllTransactions() {
        return paymentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toTransactionResponse)
                .collect(Collectors.toList());
    }

    private TransactionResponse toTransactionResponse(Payment payment) {
        String displayName = null;
        if (payment.getBuyer() != null) {
            displayName = payment.getBuyer().getTiktokUsername();
        }

        String paymentStatusLabel = switch (payment.getPaymentStatus()) {
            case SUCCESS -> "Payment successful";
            case PENDING -> "Pending";
            case FAILED -> "Failed";
        };

        String tiktokAuthStatus = payment.isTiktokAuthenticated()
                ? "TikTok authorized"
                : "TikTok unauthorized";

        return TransactionResponse.builder()
                .id(payment.getId())
                .tiktokDisplayName(displayName)
                .amount(payment.getAmount())
                .coinRate(payment.getCoinRate())
                .coinAmount(payment.getCoinAmount())
                .paymentStatus(paymentStatusLabel)
                .tiktokAuthStatus(tiktokAuthStatus)
                .paystackReference(payment.getPaystackReference())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}