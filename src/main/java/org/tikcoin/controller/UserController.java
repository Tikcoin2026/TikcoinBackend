package org.tikcoin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tikcoin.dto.response.ApiResponseDto;
import org.tikcoin.dto.response.NotificationResponse;
import org.tikcoin.dto.response.OrderResponse;
import org.tikcoin.service.NotificationService;
import org.tikcoin.service.OrderService;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final OrderService orderService;
    private final NotificationService notificationService;

    @GetMapping("/orders")
    public ResponseEntity<ApiResponseDto<List<OrderResponse>>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(ApiResponseDto.success("Orders fetched successfully", orders));
    }

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponseDto<List<NotificationResponse>>> getAllNotifications() {
        List<NotificationResponse> notifications = notificationService.getAllNotifications();
        return ResponseEntity.ok(ApiResponseDto.success("Notifications fetched successfully", notifications));
    }
}