package com.clothing.controller;

import com.clothing.dto.request.StockAlertSubscribeRequest;
import com.clothing.entity.StockAlertSubscriptionEntity;
import com.clothing.entity.UserEntity;
import com.clothing.exception.BusinessException;
import com.clothing.repository.StockAlertSubscriptionRepository;
import com.clothing.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/user/stock-alerts")
public class StockAlertController {

    private final StockAlertSubscriptionRepository stockAlertSubscriptionRepository;
    private final UserRepository userRepository;

    public StockAlertController(
            StockAlertSubscriptionRepository stockAlertSubscriptionRepository,
            UserRepository userRepository
    ) {
        this.stockAlertSubscriptionRepository = stockAlertSubscriptionRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> subscribe(
            Authentication authentication,
            @Valid @RequestBody StockAlertSubscribeRequest request
    ) {
        UserEntity user = getUser(authentication.getName());
        String normalizedSize = normalize(request.getSize());
        String normalizedColor = normalize(request.getColor());

        StockAlertSubscriptionEntity entity = stockAlertSubscriptionRepository
                .findByUserIdAndProductIdAndSizeAndColor(user.getId(), request.getProductId(), normalizedSize, normalizedColor)
                .orElseGet(() -> {
                    StockAlertSubscriptionEntity created = new StockAlertSubscriptionEntity();
                    created.setUserId(user.getId());
                    created.setProductId(request.getProductId());
                    created.setSize(normalizedSize);
                    created.setColor(normalizedColor);
                    created.setCreatedAt(LocalDateTime.now());
                    return created;
                });
        entity.setNotified(false);
        StockAlertSubscriptionEntity saved = stockAlertSubscriptionRepository.save(entity);
        return ResponseEntity.ok(Map.of("id", saved.getId(), "subscribed", true));
    }

    @DeleteMapping
    public ResponseEntity<Void> unsubscribe(
            Authentication authentication,
            @Valid @RequestBody StockAlertSubscribeRequest request
    ) {
        UserEntity user = getUser(authentication.getName());
        String normalizedSize = normalize(request.getSize());
        String normalizedColor = normalize(request.getColor());
        stockAlertSubscriptionRepository.findByUserIdAndProductIdAndSizeAndColor(
                user.getId(),
                request.getProductId(),
                normalizedSize,
                normalizedColor
        ).ifPresent(stockAlertSubscriptionRepository::delete);
        return ResponseEntity.noContent().build();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private UserEntity getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found", NOT_FOUND));
    }
}
