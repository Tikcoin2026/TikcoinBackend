package org.tikcoin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.tikcoin.dto.request.CoinRateRequest;
import org.tikcoin.dto.request.OrderRequest;
import org.tikcoin.dto.response.ApiResponseDto;
import org.tikcoin.dto.response.CoinRateResponse;
import org.tikcoin.dto.response.OrderResponse;
import org.tikcoin.dto.response.TransactionResponse;
import org.tikcoin.model.Admin;
import org.tikcoin.repository.AdminRepository;
import org.tikcoin.service.OrderService;
import org.tikcoin.service.PaymentService;
import org.tikcoin.service.RateService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final OrderService orderService;
    private final RateService rateService;
    private final PaymentService paymentService;
    private final AdminRepository adminRepository;

    @PostMapping("/rate")
    public ResponseEntity<ApiResponseDto<CoinRateResponse>> setRate(
            @Valid @RequestBody CoinRateRequest request) {
        CoinRateResponse rate = rateService.setRate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Coin rate set successfully. All orders recalculated.", rate));
    }

    @GetMapping("/rate")
    public ResponseEntity<ApiResponseDto<CoinRateResponse>> getRate() {
        CoinRateResponse rate = rateService.getCurrentRate();
        return ResponseEntity.ok(ApiResponseDto.success("Current coin rate fetched successfully", rate));
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponseDto<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request) {
        OrderResponse order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Order created successfully", order));
    }

    @PutMapping("/orders/{id}")
    public ResponseEntity<ApiResponseDto<OrderResponse>> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderRequest request) {
        OrderResponse order = orderService.updateOrder(id, request);
        return ResponseEntity.ok(ApiResponseDto.success("Order updated successfully", order));
    }

    @DeleteMapping("/orders/{id}")
    public ResponseEntity<ApiResponseDto<Void>> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok(ApiResponseDto.success("Order deleted successfully"));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponseDto<OrderResponse>> getOrder(@PathVariable Long id) {
        OrderResponse order = orderService.getOrder(id);
        return ResponseEntity.ok(ApiResponseDto.success("Order fetched successfully", order));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponseDto<List<OrderResponse>>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(ApiResponseDto.success("Orders fetched successfully", orders));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponseDto<List<TransactionResponse>>> getAllTransactions() {
        List<TransactionResponse> transactions = paymentService.getAllTransactions();
        return ResponseEntity.ok(ApiResponseDto.success("Transactions fetched successfully", transactions));
    }

    @PostMapping("/fcm-token")
    public ResponseEntity<ApiResponseDto<Void>> registerFcmToken(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails currentUser) {
        String token = body.get("fcmToken");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("fcmToken is required"));
        }

        if (currentUser == null) {
            logger.error("FCM token registration failed — no authenticated user");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponseDto.error("Authentication required"));
        }

        String username = currentUser.getUsername();
        logger.info("Registering FCM token for user: {}", username);

        var adminOpt = adminRepository.findByTiktokOpenId(username);
        if (adminOpt.isEmpty()) {
            logger.error("FCM token registration failed — no admin found with tiktokOpenId: {}", username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDto.error("Admin not found"));
        }

        Admin admin = adminOpt.get();
        admin.setFcmToken(token);
        adminRepository.save(admin);
        logger.info("FCM token saved for admin id: {}", admin.getId());

        return ResponseEntity.ok(ApiResponseDto.success("FCM token registered successfully"));
    }

    @PostMapping("/payments/{paymentId}/qr-code")
    public ResponseEntity<ApiResponseDto<TransactionResponse>> uploadQrCode(
            @PathVariable Long paymentId,
            @RequestBody Map<String, String> body) {
        String qrCode = body.get("qrCode");
        if (qrCode == null || qrCode.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponseDto.error("qrCode (base64) is required"));
        }
        TransactionResponse response = paymentService.uploadQrCode(paymentId, qrCode);
        return ResponseEntity.ok(ApiResponseDto.success("QR code uploaded. Buyer has been notified.", response));
    }

    @PostMapping("/payments/{paymentId}/complete")
    public ResponseEntity<ApiResponseDto<TransactionResponse>> markComplete(
            @PathVariable Long paymentId) {
        TransactionResponse response = paymentService.markComplete(paymentId);
        return ResponseEntity.ok(ApiResponseDto.success("Order marked as completed.", response));
    }
}