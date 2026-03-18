package com.example.notification.consumer;

import com.example.notification.dto.OrderEvent;
import com.example.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderConsumer {
    private final NotificationService notificationService;

    @RabbitListener(queues = "order_notifications")
    public void handleOrderEvent(OrderEvent event) {
        notificationService.processOrderNotification(event);
    }
}