package com.example.chat.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.example.chat.entity.Message;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationId(Long conversationId);
}