package com.ltss.service.moderation;

import com.ltss.common.response.PageResponse;
import com.ltss.dto.moderation.NotificationResponse;
import com.ltss.dto.moderation.UnreadNotificationCountResponse;

public interface NotificationService {

    PageResponse<NotificationResponse> list(int page, int size);

    UnreadNotificationCountResponse unreadCount();

    NotificationResponse markRead(Long notificationId);
}
