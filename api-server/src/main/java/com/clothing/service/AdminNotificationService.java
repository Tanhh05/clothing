package com.clothing.service;

import com.clothing.dto.request.NotificationBroadcastRequest;
import com.clothing.dto.response.AdminNotificationResponse;

import java.util.List;

public interface AdminNotificationService {

    List<AdminNotificationResponse> getHistory();

    AdminNotificationResponse create(NotificationBroadcastRequest request);
}
