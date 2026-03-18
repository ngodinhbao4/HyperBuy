package com.example.notification.service;

import com.example.notification.client.UserServiceClient;
import com.example.notification.dto.OrderEvent;
import com.example.notification.dto.UserResponse;
import com.example.notification.dto.response.ApiResponRequest;
import com.example.notification.entity.Notification;
import com.example.notification.repository.NotificationRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import feign.FeignException;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final FirebaseMessaging firebaseMessaging;
    private final UserServiceClient userServiceClient;
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    // Xử lý thông báo từ đơn hàng
    public void processOrderNotification(OrderEvent event) {
        logger.debug("Processing OrderEvent: {}", event);
        if (event.getUserId() == null) {
            logger.error("OrderEvent userId is null: {}", event);
            return;
        }
        if (event.getAuthorizationHeader() == null) {
            logger.error("OrderEvent authorizationHeader is null: {}", event);
            return;
        }
        try {
            logger.debug("Calling UserService for userId: {}", event.getUserId());
            ApiResponRequest<UserResponse> response = userServiceClient.getUserById(event.getUserId(), event.getAuthorizationHeader());
            if (response.getResult() == null || response.getResult().getId() == null) {
                logger.error("UserResponse or id is null for userId: {}", event.getUserId());
                return;
            }
            String userId = response.getResult().getId();
            logger.debug("Received UserResponse: {}", response.getResult());
            
            // Tạo nội dung thông báo dựa trên trạng thái
            String messageContent;
            if ("PENDING".equals(event.getStatus())) {
                messageContent = String.format("Đơn hàng #%s đã được tạo thành công", event.getId());
            } else {
                messageContent = String.format("Đơn hàng #%s hiện đang %s", event.getId(), event.getStatus().toLowerCase());
            }

            try {
                sendPushNotification(userId, messageContent);
            } catch (Exception e) {
                logger.error("Failed to send push notification for userId: {}. Error: {}", userId, e.getMessage(), e);
                // Tiếp tục lưu thông báo dù push notification thất bại
            }

            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setOrderId(String.valueOf(event.getId()));
            notification.setMessage(messageContent);
            notification.setType("ORDER");
            notification.setStatus("SENT");
            logger.debug("Saving notification: {}", notification);
            notificationRepository.save(notification);
        } catch (FeignException e) {
            logger.error("Failed to fetch user for userId: {}. Error: {}", event.getUserId(), e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to process OrderEvent: {}. Error: {}", event, e.getMessage());
        }
    }

    // Admin tạo thông báo
    public void createAdminNotification(String userId, String message, String authorizationHeader) {
        logger.debug("Creating admin notification for userId: {}, message: {}", userId, message);
        try {
            // Kiểm tra user tồn tại
            logger.debug("Calling UserService for userId: {}", userId);
            ApiResponRequest<UserResponse> response = userServiceClient.getUserById(userId, authorizationHeader);
            if (response.getResult() == null || response.getResult().getId() == null) {
                logger.error("UserResponse or id is null for userId: {}", userId);
                throw new RuntimeException("User not found for userId: " + userId);
            }
            String validatedUserId = response.getResult().getId();
            logger.debug("Validated userId: {}", validatedUserId);

            // Gửi push notification
            try {
                logger.debug("Sending push notification for userId: {}", validatedUserId);
                sendPushNotification(validatedUserId, message);
            } catch (Exception e) {
                logger.error("Failed to send push notification for userId: {}. Error: {}", validatedUserId, e.getMessage(), e);
                // Tiếp tục lưu thông báo dù push notification thất bại
            }

            // Lưu thông báo
            Notification notification = new Notification();
            notification.setUserId(validatedUserId);
            notification.setMessage(message);
            notification.setType("ADMIN");
            notification.setStatus("SENT");
            logger.debug("Saving notification: {}", notification);
            notificationRepository.save(notification);
        } catch (FeignException e) {
            logger.error("Failed to fetch user for userId: {}. Error: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to fetch user: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to create admin notification for userId: {}. Error: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to send notification: " + e.getMessage());
        }
    }

    // Đánh dấu thông báo đã đọc
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    // Lấy danh sách thông báo của user
    private void sendPushNotification(String userId, String content) {
        try {
            Message message = Message.builder()
                    .putData("title", "Admin Notification")
                    .putData("body", content)
                    .setTopic(userId)
                    .build();
            String response = firebaseMessaging.send(message);
            logger.debug("Push notification sent successfully for userId: {}, response: {}", userId, response);
        } catch (FirebaseMessagingException e) {
            logger.error("Firebase error for userId: {}. Code: {}, Message: {}", userId, e.getErrorCode(), e.getMessage());
            throw new RuntimeException("Failed to send push notification", e);
        } catch (Exception e) {
            logger.error("Unexpected error sending push notification for userId: {}. Error: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to send push notification", e);
        }
    }

    public List<Notification> getUserNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}