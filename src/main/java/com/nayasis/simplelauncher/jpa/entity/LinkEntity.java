package com.nayasis.simplelauncher.jpa.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.nayasis.basica.model.NDate;
import com.github.nayasis.basica.reflection.Reflector;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.Set;

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
    @Column @Type(type="yes_no")
    private Boolean showConsole;
    @Column
    private String option;
    @Column
    private String optionPrefix;
    @Column
    private String commandPrev;
    @Column
    private String commandNext;
    @Column(name="desc", columnDefinition="CLOB")
    private String description;
    @Column(columnDefinition="CLOB")
    private String keyword;
    @Column(length=2000)
    private byte[] icon;
    @Column
    private Integer execCount;
    @Column
    private LocalDateTime lastExecDate;

    public void setLastExecDate( LocalDateTime lastExecDate ) {
        this.lastExecDate = lastExecDate;
    }

    @JsonIgnore
    public void setLastExecDate( NDate lastExecDate ) {
        this.lastExecDate = lastExecDate == null ? null : lastExecDate.toLocalDateTime();
    }

    public void setKeyword( String keyword ) {
        this.keyword = keyword;
    }

    public void setKeyword( Set<String> keyword ) {
        this.keyword = Reflector.toJson( keyword );
    }

}
