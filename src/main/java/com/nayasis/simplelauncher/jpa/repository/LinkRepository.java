package com.nayasis.simplelauncher.jpa.repository;

import com.nayasis.simplelauncher.jpa.entity.LinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LinkRepository extends JpaRepository<LinkEntity,Long> {
}
