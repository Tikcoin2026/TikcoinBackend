package org.tikcoin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.tikcoin.dto.request.PaymentInitRequest;
import org.tikcoin.dto.response.ApiResponseDto;
import org.tikcoin.dto.response.PaymentInitResponse;
import org.tikcoin.dto.response.TransactionResponse;
import org.tikcoin.service.PaymentService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;

    @PostMapping("/payment/initialize")
    public ResponseEntity<ApiResponseDto<PaymentInitResponse>> initializePayment(
            @Valid @RequestBody PaymentInitRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        PaymentInitResponse response = paymentService.initializePayment(request, currentUser.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Payment initialized successfully", response));
    }

    @GetMapping("/payment/verify/{reference}")
    public ResponseEntity<ApiResponseDto<TransactionResponse>> verifyPayment(
            @PathVariable String reference) {
        TransactionResponse transaction = paymentService.verifyPayment(reference);
        return ResponseEntity.ok(ApiResponseDto.success("Payment verified", transaction));
    }

    @GetMapping("/payment/{reference}/status")
    public ResponseEntity<ApiResponseDto<TransactionResponse>> getPaymentStatus(
            @PathVariable String reference) {
        TransactionResponse response = paymentService.getPaymentStatus(reference);
        return ResponseEntity.ok(ApiResponseDto.success("Payment status fetched", response));
    }

    @PostMapping("/webhook/paystack")
    public ResponseEntity<Void> handlePaystackWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "x-paystack-signature", required = false) String signature) {

        logger.info("Paystack webhook received — signature present: {}", signature != null);
        logger.info("Paystack webhook payload: {}", payload);

        try {
            paymentService.handleWebhook(payload, signature);
        } catch (Exception e) {
            logger.error("Paystack webhook error: {}", e.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}