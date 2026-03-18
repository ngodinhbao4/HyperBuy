package com.example.notification.controller;

import com.example.notification.dto.request.NotificationRequest;
import com.example.notification.dto.response.ApiResponRequest;
import com.example.notification.entity.Notification;
import com.example.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @RequestMapping(value = "/admin/send", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptions() {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/send")
    public ApiResponRequest<String> sendAdminNotification(@RequestBody NotificationRequest request, 
                                                        @RequestHeader("Authorization") String authorizationHeader) {
        notificationService.createAdminNotification(request.getUserId(), request.getMessage(), authorizationHeader);
        return ApiResponRequest.<String>builder()
                .code(1000)
                .result("Notification sent successfully")
                .build();
    }

    @GetMapping("/{userId}")
    public ApiResponRequest<List<Notification>> getUserNotifications(@PathVariable("userId") String userId) {
        return ApiResponRequest.<List<Notification>>builder()
                .code(1000)
                .result(notificationService.getUserNotifications(userId))
                .build();
    }

    // User đánh dấu thông báo đã đọc
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.noContent().build();
    }
}