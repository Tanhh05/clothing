package com.clothing.service;

import com.clothing.dto.response.UserNotificationResponse;

import java.util.List;
import java.util.Map;

public interface UserNotificationService {

    List<UserNotificationResponse> getMyNotifications(String username);

    Map<String, Long> getUnreadCount(String username);

    void markAsRead(String username, Long id);

    void markAllAsRead(String username);
}
