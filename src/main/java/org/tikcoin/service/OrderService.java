package org.tikcoin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tikcoin.dto.request.OrderRequest;
import org.tikcoin.dto.response.OrderResponse;
import org.tikcoin.exception.ResourceNotFoundException;
import org.tikcoin.model.Order;
import org.tikcoin.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final RateService rateService;

    public OrderResponse createOrder(OrderRequest request) {
        BigDecimal currentRate = rateService.getCurrentNairaPerCoin();
        Order order = Order.builder()
                .rate(currentRate)
                .amount(request.getCoinAmount())
                .totalAmount(currentRate.multiply(BigDecimal.valueOf(request.getCoinAmount())))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return toResponse(orderRepository.save(order));
    }

    public OrderResponse updateOrder(Long id, OrderRequest request) {
        Order order = findOrThrow(id);
        BigDecimal currentRate = rateService.getCurrentNairaPerCoin();
        order.setAmount(request.getCoinAmount());
        order.setRate(currentRate);
        order.setTotalAmount(currentRate.multiply(BigDecimal.valueOf(request.getCoinAmount())));
        order.setUpdatedAt(LocalDateTime.now());
        return toResponse(orderRepository.save(order));
    }

    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Order not found with id: " + id);
        }
        orderRepository.deleteById(id);
    }

    public OrderResponse getOrder(Long id) {
        return toResponse(findOrThrow(id));
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    private Order findOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .rate(order.getRate())
                .amount(order.getAmount())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}