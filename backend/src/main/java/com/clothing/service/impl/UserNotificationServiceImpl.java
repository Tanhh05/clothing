package com.clothing.service.impl;

import com.clothing.dto.response.UserNotificationResponse;
import com.clothing.entity.NotificationEntity;
import com.clothing.entity.UserEntity;
import com.clothing.exception.BusinessException;
import com.clothing.repository.NotificationRepository;
import com.clothing.repository.UserRepository;
import com.clothing.service.UserNotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class UserNotificationServiceImpl implements UserNotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public UserNotificationServiceImpl(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<UserNotificationResponse> getMyNotifications(String username) {
        UserEntity user = getUser(username);
        return notificationRepository.findTop50ByUserIdOrderByIdDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public Map<String, Long> getUnreadCount(String username) {
        UserEntity user = getUser(username);
        long unread = notificationRepository.countByUserIdAndIsReadFalse(user.getId());
        return Map.of("unread", unread);
    }

    @Override
    @Transactional
    public void markAsRead(String username, Long id) {
        UserEntity user = getUser(username);
        NotificationEntity entity = notificationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Notification not found", HttpStatus.NOT_FOUND));
        if (!user.getId().equals(entity.getUserId())) {
            throw new BusinessException("Not allowed", HttpStatus.FORBIDDEN);
        }
        entity.setIsRead(true);
        notificationRepository.save(entity);
    }

    @Override
    @Transactional
    public void markAllAsRead(String username) {
        UserEntity user = getUser(username);
        List<NotificationEntity> notifications = notificationRepository.findTop50ByUserIdOrderByIdDesc(user.getId());
        boolean changed = false;
        for (NotificationEntity notification : notifications) {
            if (!Boolean.TRUE.equals(notification.getIsRead())) {
                notification.setIsRead(true);
                changed = true;
            }
        }
        if (changed) {
            notificationRepository.saveAll(notifications);
        }
    }

    private UserEntity getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
    }

    private UserNotificationResponse toResponse(NotificationEntity entity) {
        return UserNotificationResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .type(entity.getType())
                .isRead(Boolean.TRUE.equals(entity.getIsRead()))
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
