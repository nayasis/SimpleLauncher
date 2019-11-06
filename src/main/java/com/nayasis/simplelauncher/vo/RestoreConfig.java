package com.nayasis.simplelauncher.vo;

import io.nayasis.basica.base.Strings;
import io.nayasis.basicafx.javafx.properties.StageProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

@Data
@Slf4j
public class RestoreConfig implements Serializable {

    private static final long serialVersionUID = 8729130737146250593L;

    private StageProperties mainStageProperties;
    private int             focusedRow;

    public RestoreConfig() {}

    public RestoreConfig( String encodedString ) {
        deserialize( encodedString );
    }

    public String serialize() {
        return Strings.encode( this );
    }

    public void deserialize( String encodedString ) {
        if( Strings.isEmpty(encodedString) ) return;
        try {
            RestoreConfig config = (RestoreConfig) Strings.decode( encodedString );
            setMainStageProperties( config.mainStageProperties );
            setFocusedRow( config.focusedRow );
        } catch ( Exception e ) {
            log.error( e.getMessage(), e );
        }
    }

}
