package com.example.chat.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import com.example.chat.dto.response.ApiResponse;
import com.example.chat.dto.response.MessageResponse;
import com.example.chat.entity.Conversation;
import com.example.chat.entity.Message;
import com.example.chat.service.ChatService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping("/conversations")
    public ApiResponse<Conversation> createConversation(
            @RequestParam String participant1Id,
            @RequestParam String participant2Id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        try {
            String token = authorization.replace("Bearer ", "");
            Conversation conversation = chatService.createConversation(participant1Id, participant2Id, token);
            return ApiResponse.<Conversation>builder()
                    .code(1000)
                    .message("Conversation created successfully")
                    .result(conversation)
                    .build();
        } catch (IllegalArgumentException e) {
            return ApiResponse.<Conversation>builder()
                    .code(4000)
                    .message(e.getMessage())
                    .result(null)
                    .build();
        }
    }

    @GetMapping("/messages/{conversationId}")
    public ApiResponse<List<MessageResponse>> getMessages(
            @PathVariable Long conversationId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam String userId) {
        try {
            String token = authorization.replace("Bearer ", "");
            List<Message> messages = chatService.getMessages(conversationId, token, userId);
            List<MessageResponse> response = messages.stream()
                    .map(msg -> {
                        MessageResponse msgResponse = new MessageResponse();
                        msgResponse.setId(msg.getId());
                        msgResponse.setConversationId(msg.getConversationId());
                        msgResponse.setSenderId(msg.getSenderId());
                        msgResponse.setContent(msg.getContent());
                        msgResponse.setSentAt(msg.getSentAt());
                        msgResponse.setRead(msg.isRead());
                        return msgResponse;
                    })
                    .collect(Collectors.toList());
            return ApiResponse.<List<MessageResponse>>builder()
                    .code(1000)
                    .message("Messages retrieved successfully")
                    .result(response)
                    .build();
        } catch (IllegalArgumentException e) {
            return ApiResponse.<List<MessageResponse>>builder()
                    .code(4000)
                    .message(e.getMessage())
                    .result(null)
                    .build();
        }
    }

    @PostMapping("/sendMessage")
    public ApiResponse<MessageResponse> sendMessage(
            @RequestBody Message message,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        try {
            String token = authorization.replace("Bearer ", "");
            Message savedMessage = chatService.saveMessage(
                    message.getConversationId(),
                    message.getSenderId(),
                    message.getContent(),
                    token);
            MessageResponse response = new MessageResponse();
            response.setId(savedMessage.getId());
            response.setConversationId(savedMessage.getConversationId());
            response.setSenderId(savedMessage.getSenderId());
            response.setContent(savedMessage.getContent());
            response.setSentAt(savedMessage.getSentAt());
            response.setRead(savedMessage.isRead());

            messagingTemplate.convertAndSend("/topic/messages", response);

            return ApiResponse.<MessageResponse>builder()
                    .code(1000)
                    .message("Message sent successfully")
                    .result(response)
                    .build();
        } catch (IllegalArgumentException e) {
            return ApiResponse.<MessageResponse>builder()
                    .code(4000)
                    .message(e.getMessage())
                    .result(null)
                    .build();
        }
    }

    @MessageMapping("/sendMessage")
    @SendTo("/topic/messages")
    public MessageResponse sendMessageViaWebSocket(
            @Payload Message message,
            @RequestHeader("Authorization") String authorization) {
        try {
            String token = authorization.replace("Bearer ", "");
            Message savedMessage = chatService.saveMessage(
                    message.getConversationId(),
                    message.getSenderId(),
                    message.getContent(),
                    token);
            MessageResponse response = new MessageResponse();
            response.setId(savedMessage.getId());
            response.setConversationId(savedMessage.getConversationId());
            response.setSenderId(savedMessage.getSenderId());
            response.setContent(savedMessage.getContent());
            response.setSentAt(savedMessage.getSentAt());
            response.setRead(savedMessage.isRead());
            return response;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}