package com.nayasis.simplelauncher.jpa.repository;

import com.nayasis.simplelauncher.jpa.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message,Long> {
}
