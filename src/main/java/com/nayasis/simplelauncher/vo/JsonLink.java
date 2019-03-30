package com.nayasis.simplelauncher.vo;

import com.nayasis.simplelauncher.jpa.entity.LinkEntity;
import io.nayasis.common.base.Strings;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

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
        entity.setExecCount( getExecCount() );
        entity.setLastExecDate( getLastExecDate() );

        try {
            entity.setIcon( (byte[]) Strings.decode( getIcon() ) );
        } catch( ClassCastException e ) {}

        return entity;

    }

}
