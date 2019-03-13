package com.nayasis.simplelauncher.jpa.repository;

import com.nayasis.simplelauncher.jpa.entity.ConfigEntity;
import com.nayasis.simplelauncher.jpa.entity.LinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfigRepository extends JpaRepository<ConfigEntity,Long> {

    ConfigEntity findByKey( String key );

    void deleteByKey( Long id );

}
