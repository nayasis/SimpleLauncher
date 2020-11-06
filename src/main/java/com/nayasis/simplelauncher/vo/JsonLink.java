package com.nayasis.simplelauncher.vo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.github.nayasis.basica.base.Strings;
import com.nayasis.simplelauncher.common.Commons;
import com.nayasis.simplelauncher.jpa.entity.LinkEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.DISABLED;

@Data
@ToString
@NoArgsConstructor
public class JsonLink {

    private String        title;
    private String        group;
    private String        path;
    private String        relativePath;
    private Boolean       showConsole;
    private String        option;
    private String        optionPrefix;
    private String        commandPrev;
    private String        commandNext;
    private String        description;
    private String        keyword;
    private String        icon;
    private Integer       execCount;
    private LocalDateTime lastExecDate;

    @JsonCreator( mode = DISABLED )
    public JsonLink( LinkEntity entity ) {
        this.title        = entity.getTitle();
        this.group        = entity.getGrp();
        this.path         = entity.getPath();
        this.relativePath = entity.getRelativePath();
        this.showConsole  = entity.getShowConsole();
        this.option       = entity.getOption();
        this.optionPrefix = entity.getOptionPrefix();
        this.commandPrev  = entity.getCommandPrev();
        this.commandNext  = entity.getCommandNext();
        this.description  = entity.getDescription();
        this.keyword      = entity.getKeyword();
        this.icon         = Strings.encode( entity.getIcon() );
        this.execCount    = entity.getExecCount();
        this.lastExecDate = entity.getLastExecDate();
    }

    public LinkEntity toLinkEntity() {

        LinkEntity entity = new LinkEntity();

        entity.setTitle( getTitle() );
        entity.setGrp( getGroup() );
        entity.setPath( getPath() );
        entity.setRelativePath( getRelativePath() );
        entity.setShowConsole( getShowConsole() );
        entity.setOption( getOption() );
        entity.setOptionPrefix( getOptionPrefix() );
        entity.setCommandNext( getCommandNext() );
        entity.setCommandPrev( getCommandPrev() );
        entity.setDescription( getDescription() );
        entity.setKeyword( getKeyword() );
        entity.setExecCount( getExecCount() );
        entity.setLastExecDate( getLastExecDate() );
        entity.setKeyword( Commons.getKeyword(group, keyword, description) );

        setIcon( entity );

        return entity;

    }

    public void setIcon( String icon ) {
        this.icon = icon;
    }

    private void setIcon( LinkEntity entity ) {
        try {
            entity.setIcon( (byte[]) Strings.decode( icon ) );
        } catch( ClassCastException e ) {}
    }

    public void setLastExecDate( LocalDateTime lastExecDate ) {
        this.lastExecDate = lastExecDate;
    }
}
