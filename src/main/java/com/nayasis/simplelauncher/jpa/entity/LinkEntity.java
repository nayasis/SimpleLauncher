package com.nayasis.simplelauncher.jpa.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LinkEntity {

    @Id @GeneratedValue
    private Long id;

    @Column
    private String title;
    @Column
    private String group;
    @Column
    private String path;
    @Column
    private String relativePath;
    @Column
    private String option;
    @Column
    private String optionPrefix;
    @Column
    private String commandPrev;
    @Column
    private String commandNext;
    @Column
    private String description;
    @Column
    private Byte[] icon;
    @Column
    private Integer execCount;
    @Column
    private LocalDateTime lastExecDate;

}
