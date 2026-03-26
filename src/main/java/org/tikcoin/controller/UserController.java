package org.tikcoin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.tikcoin.dto.response.ApiResponseDto;
import org.tikcoin.dto.response.OrderResponse;
import org.tikcoin.service.OrderService;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class UserController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponseDto<List<OrderResponse>>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(ApiResponseDto.success("Orders fetched successfully", orders));
    }
}