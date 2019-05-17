package com.nayasis.simplelauncher.vo;

import io.nayasis.common.basica.base.Strings;
import io.nayasis.common.basicafx.javafx.properties.StageProperties;
import lombok.Data;

import java.io.Serializable;

@Data
public class RestoreConfig implements Serializable {

    private static final long serialVersionUID = 8729130737146250593L;

    private StageProperties         mainStageProperties;
    private int                     focusedRow;

    public RestoreConfig() {}

    public RestoreConfig( String encodedString ) {
        deserialize( encodedString );
    }

    public String serialize() {
        return Strings.encode( this );
    }

    public void deserialize( String encodedString ) {

        if( Strings.isEmpty(encodedString) ) return;

        RestoreConfig config = (RestoreConfig) Strings.decode( encodedString );
        setMainStageProperties( config.mainStageProperties );
        setFocusedRow( config.focusedRow );

    }

}
