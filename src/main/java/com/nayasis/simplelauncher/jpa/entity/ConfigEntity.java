package com.nayasis.simplelauncher.jpa.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Data
@NoArgsConstructor
@ToString
public class ConfigEntity {

    @Id
    private String key;

    @Column(columnDefinition="CLOB")
    private String value;

    public ConfigEntity( String key ) {
        this.key = key;
    }

}