package com.example.chat.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.chat.client.UserServiceClient;
import com.example.chat.entity.Conversation;
import com.example.chat.entity.Message;
import com.example.chat.repository.ConversationRepository;
import com.example.chat.repository.MessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ChatService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserServiceClient userServiceClient;

    public Conversation createConversation(String participant1Id, String participant2Id, String token) {
        // Xác thực cả hai người dùng
        if (!userServiceClient.validateUser(participant1Id, token) || !userServiceClient.validateUser(participant2Id, token)) {
            throw new IllegalArgumentException("Invalid user ID");
        }

        // Kiểm tra xem cuộc trò chuyện đã tồn tại chưa
        Optional<Conversation> existingConversation = conversationRepository
                .findByParticipant1IdAndParticipant2Id(participant1Id, participant2Id)
                .or(() -> conversationRepository.findByParticipant2IdAndParticipant1Id(participant1Id, participant2Id));

        if (existingConversation.isPresent()) {
            return existingConversation.get();
        }

        Conversation conversation = new Conversation();
        conversation.setParticipant1Id(participant1Id);
        conversation.setParticipant2Id(participant2Id);
        conversation.setCreatedAt(LocalDateTime.now());
        return conversationRepository.save(conversation);
    }

    public Message saveMessage(Long conversationId, String senderId, String content, String token) {
        // Xác thực người gửi
        if (!userServiceClient.validateUser(senderId, token)) {
            throw new IllegalArgumentException("Invalid sender ID");
        }

        // Kiểm tra cuộc trò chuyện tồn tại và người gửi thuộc cuộc trò chuyện
        Optional<Conversation> conversation = conversationRepository.findById(conversationId);
        if (conversation.isEmpty() ||
            (!conversation.get().getParticipant1Id().equals(senderId) &&
            !conversation.get().getParticipant2Id().equals(senderId))) {
            throw new IllegalArgumentException("Invalid conversation ID or sender not in conversation");
        }

        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setSentAt(LocalDateTime.now());
        message.setRead(false);
        return messageRepository.save(message);
    }

    public List<Message> getMessages(Long conversationId, String token, String userId) {
        // Xác thực người dùng
        if (!userServiceClient.validateUser(userId, token)) {
            throw new IllegalArgumentException("Invalid user ID");
        }

        // Kiểm tra quyền truy cập cuộc trò chuyện
        Optional<Conversation> conversation = conversationRepository.findById(conversationId);
        if (conversation.isEmpty() || 
            (!conversation.get().getParticipant1Id().equals(userId) && 
            !conversation.get().getParticipant2Id().equals(userId))) {
            throw new IllegalArgumentException("User not part of this conversation");
        }

        return messageRepository.findByConversationId(conversationId);
    }
}