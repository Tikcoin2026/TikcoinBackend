package org.tikcoin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tikcoin.dto.request.CoinRateRequest;
import org.tikcoin.dto.response.CoinRateResponse;
import org.tikcoin.dto.response.NotificationResponse;
import org.tikcoin.dto.response.OrderResponse;
import org.tikcoin.exception.ResourceNotFoundException;
import org.tikcoin.model.CoinRate;
import org.tikcoin.model.Notification;
import org.tikcoin.model.Order;
import org.tikcoin.repository.CoinRateRepository;
import org.tikcoin.repository.OrderRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RateService {

    private final CoinRateRepository coinRateRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    public BigDecimal getCurrentNairaPerCoin() {
        return coinRateRepository.findTopByOrderByCreatedAtDesc()
                .map(CoinRate::getNairaPerCoin)
                .orElseThrow(() -> new ResourceNotFoundException("No coin rate has been set yet. Please set a rate first."));
    }

    public CoinRateResponse getCurrentRate() {
        CoinRate rate = coinRateRepository.findTopByOrderByCreatedAtDesc()
                .orElseThrow(() -> new ResourceNotFoundException("No coin rate has been set yet."));
        return toResponse(rate);
    }

    @Transactional
    public CoinRateResponse setRate(CoinRateRequest request) {
        BigDecimal nairaPerCoin = request.getAmount()
                .divide(request.getRate(), 10, RoundingMode.HALF_UP);

        CoinRate coinRate = CoinRate.builder()
                .rate(request.getRate())
                .amount(request.getAmount())
                .nairaPerCoin(nairaPerCoin)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        coinRateRepository.save(coinRate);

        List<Order> orders = orderRepository.findAll();
        for (Order order : orders) {
            order.setRate(nairaPerCoin);
            order.setTotalAmount(BigDecimal.valueOf(order.getAmount()).multiply(nairaPerCoin));
            order.setUpdatedAt(LocalDateTime.now());
        }
        List<Order> updatedOrders = orderRepository.saveAll(orders);

        CoinRateResponse rateResponse = toResponse(coinRate);

        Notification notification = notificationService.createRateChangeNotification(nairaPerCoin, request.getRate());
        NotificationResponse notificationResponse = NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .timestamp(notification.getTimestamp())
                .build();

        messagingTemplate.convertAndSend("/topic/rate", rateResponse);

        List<OrderResponse> orderResponses = updatedOrders.stream()
                .map(this::toOrderResponse)
                .toList();
        messagingTemplate.convertAndSend("/topic/orders", orderResponses);

        messagingTemplate.convertAndSend("/topic/notifications", notificationResponse);

        return rateResponse;
    }

    private CoinRateResponse toResponse(CoinRate coinRate) {
        return CoinRateResponse.builder()
                .id(coinRate.getId())
                .rate(coinRate.getRate())
                .amount(coinRate.getAmount())
                .nairaPerCoin(coinRate.getNairaPerCoin())
                .createdAt(coinRate.getCreatedAt())
                .updatedAt(coinRate.getUpdatedAt())
                .build();
    }

    private OrderResponse toOrderResponse(Order order) {
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