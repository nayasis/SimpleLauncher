package com.nayasis.simplelauncher.jpa.repository;

import com.nayasis.simplelauncher.jpa.entity.LinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LinkRepository extends JpaRepository<LinkEntity,Long> {

    void deleteById( Long id );

    List<LinkEntity> findAllByOrderByGrpAscTitleAsc();

}
