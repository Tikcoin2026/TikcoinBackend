package org.tikcoin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.tikcoin.dto.request.PaymentInitRequest;
import org.tikcoin.dto.response.ApiResponseDto;
import org.tikcoin.dto.response.PaymentInitResponse;
import org.tikcoin.service.PaymentService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Initializes a Paystack payment for the selected order.
     * Requires TikTok authentication (JWT Bearer token).
     */
    @PostMapping("/payment/initialize")
    public ResponseEntity<ApiResponseDto<PaymentInitResponse>> initializePayment(
            @Valid @RequestBody PaymentInitRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        PaymentInitResponse response = paymentService.initializePayment(request, currentUser.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Payment initialized successfully", response));
    }

    /**
     * Paystack webhook handler. Called by Paystack when a payment event occurs.
     * Signature is verified using HMAC-SHA512.
     */
    @PostMapping("/webhook/paystack")
    public ResponseEntity<Void> handlePaystackWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "x-paystack-signature", required = false) String signature) {

        if (signature == null || signature.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}