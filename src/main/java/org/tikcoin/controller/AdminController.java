package org.tikcoin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.tikcoin.dto.request.CoinRateRequest;
import org.tikcoin.dto.request.OrderRequest;
import org.tikcoin.dto.response.ApiResponseDto;
import org.tikcoin.dto.response.CoinRateResponse;
import org.tikcoin.dto.response.OrderResponse;
import org.tikcoin.service.OrderService;
import org.tikcoin.service.RateService;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final OrderService orderService;
    private final RateService rateService;

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
}