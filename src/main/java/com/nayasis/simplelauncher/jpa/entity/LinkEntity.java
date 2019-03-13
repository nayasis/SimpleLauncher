package com.nayasis.simplelauncher.jpa.entity;

import io.nayasis.common.model.NDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    indexes = {
        @Index( name="relativePath", columnList = "relativePath" )
    }
)
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
    private String grp;
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
    private String keyword;
    @Column
    private byte[] icon;
    @Column
    private Integer execCount;
    @Column
    private LocalDateTime lastExecDate;

    public void setLastExecDate( LocalDateTime lastExecDate ) {
        this.lastExecDate = lastExecDate;
    }

    public void setLastExecDate( NDate lastExecDate ) {
        this.lastExecDate = lastExecDate == null ? null : lastExecDate.toLocalDateTime();
    }

}
