package com.example.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.chat.entity.Conversation;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByParticipant1IdAndParticipant2Id(String participant1Id, String participant2Id);
    Optional<Conversation> findByParticipant2IdAndParticipant1Id(String participant1Id, String participant2Id);
}