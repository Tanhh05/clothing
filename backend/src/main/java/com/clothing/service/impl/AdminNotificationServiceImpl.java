package com.clothing.service.impl;

import com.clothing.dto.request.NotificationBroadcastRequest;
import com.clothing.dto.response.AdminNotificationResponse;
import com.clothing.entity.NotificationEntity;
import com.clothing.exception.BusinessException;
import com.clothing.repository.NotificationRepository;
import com.clothing.repository.OrderRepository;
import com.clothing.repository.UserRepository;
import com.clothing.repository.WishlistRepository;
import com.clothing.service.AdminNotificationService;
import com.clothing.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AdminNotificationServiceImpl implements AdminNotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final WishlistRepository wishlistRepository;
    private final AuditLogService auditLogService;

    public AdminNotificationServiceImpl(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            OrderRepository orderRepository,
            WishlistRepository wishlistRepository,
            AuditLogService auditLogService
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.wishlistRepository = wishlistRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public List<AdminNotificationResponse> getHistory() {
        return notificationRepository.findTop200ByUserIdIsNullOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public AdminNotificationResponse create(NotificationBroadcastRequest request) {
        String sendMode = request.getSendMode().trim().toUpperCase(Locale.ROOT);
        LocalDateTime now = LocalDateTime.now();
        String audience = request.getAudience().trim().toUpperCase(Locale.ROOT);
        String channel = request.getChannel().trim().toUpperCase(Locale.ROOT);
        String title = request.getTitle().trim();
        String content = request.getContent().trim();

        NotificationEntity entity = new NotificationEntity();
        entity.setUserId(null);
        entity.setTitle(title);
        entity.setContent(content);
        entity.setType("ADMIN_BROADCAST");
        entity.setAudience(audience);
        entity.setChannel(channel);
        entity.setIsRead(false);
        entity.setCreatedAt(now);

        boolean deliverNow;
        if ("SCHEDULED".equals(sendMode)) {
            if (request.getScheduledAt() == null) {
                throw new BusinessException("scheduledAt is required when sendMode is SCHEDULED", HttpStatus.BAD_REQUEST);
            }
            entity.setStatus("SCHEDULED");
            entity.setScheduledAt(request.getScheduledAt());
            deliverNow = false;
        } else {
            entity.setStatus("SENT");
            entity.setScheduledAt(null);
            deliverNow = true;
        }

        NotificationEntity saved = notificationRepository.save(entity);
        if (deliverNow) {
            createUserNotifications(audience, title, content, channel, now);
        }
        auditLogService.log("NOTIFICATION_CREATED", "NOTIFICATION", saved.getId(), "Created notification " + saved.getTitle());
        return toResponse(saved);
    }

    private void createUserNotifications(String audience, String title, String content, String channel, LocalDateTime now) {
        Set<Long> userIds = resolveAudienceUserIds(audience);
        if (userIds.isEmpty()) {
            return;
        }

        List<NotificationEntity> entities = userIds.stream().map(userId -> {
            NotificationEntity item = new NotificationEntity();
            item.setUserId(userId);
            item.setTitle(title);
            item.setContent(content);
            item.setType("BROADCAST");
            item.setAudience(audience);
            item.setChannel(channel);
            item.setStatus("SENT");
            item.setScheduledAt(null);
            item.setIsRead(false);
            item.setCreatedAt(now);
            return item;
        }).toList();
        notificationRepository.saveAll(entities);
    }

    private Set<Long> resolveAudienceUserIds(String audience) {
        if ("PURCHASED".equals(audience)) {
            return new HashSet<>(orderRepository.findDistinctUserIds());
        }
        if ("WISHLIST".equals(audience)) {
            return new HashSet<>(wishlistRepository.findDistinctUserIdsWithItems());
        }
        return new HashSet<>(userRepository.findActiveCustomerUserIds());
    }

    private AdminNotificationResponse toResponse(NotificationEntity entity) {
        return AdminNotificationResponse.builder()
                .id(entity.getId())
                .createdAt(entity.getCreatedAt())
                .title(entity.getTitle())
                .audience(entity.getAudience())
                .channel(entity.getChannel())
                .status(entity.getStatus())
                .build();
    }
}
