package com.nayasis.simplelauncher.vo;

import com.nayasis.simplelauncher.common.Commons;
import com.nayasis.simplelauncher.jpa.entity.LinkEntity;
import io.nayasis.common.basica.base.Strings;
import io.nayasis.common.basica.model.NDate;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Data
@ToString
@NoArgsConstructor
public class OldJsonLink {

    private String  id;
    private String  title;
    private String  groupName;
    private String  execPath;
    private String  execPathRelative;
    private String  execOption;
    private String  execOptionPrefix;
    private String  cmdPrev;
    private String  cmdNext;
    private String  description;
    private String  keyword;
    private String  iconString;
    private Integer execCount;
    private String  lastUsedDt;

    public LinkEntity toLinkEntity() {

        LinkEntity entity = new LinkEntity();

        entity.setTitle( getTitle() );
        entity.setGrp( getGroupName() );
        entity.setPath( getExecPath() );
        entity.setRelativePath( getExecPathRelative() );
        entity.setOption( getExecOption() );
        entity.setOptionPrefix( getExecOptionPrefix() );
        entity.setCommandNext( getCmdNext() );
        entity.setCommandPrev( getCmdPrev() );
        entity.setDescription( getDescription() );
        entity.setExecCount( getExecCount() );
        entity.setLastExecDate( new NDate(getLastUsedDt()) );

        setKeyword( entity );
        setIconString( entity );

        return entity;

    }

    public void setKeyword( String keyword ) {
        this.keyword = keyword;
    }

    private void setKeyword( LinkEntity entity ) {
        entity.setKeyword( Commons.getKeyword(groupName, keyword, description) );
    }

    public void setIconString( String iconString ) {
        this.iconString = iconString;
    }

    private void setIconString( LinkEntity entity ) {
        try {
            entity.setIcon( (byte[]) Strings.decode( iconString ) );
        } catch( ClassCastException e ) {}
    }

}
