package com.nayasis.simplelauncher.common;

import io.nayasis.common.basica.base.Strings;
import io.nayasis.common.basica.base.Types;

import java.util.LinkedHashSet;
import java.util.Set;

public class Commons {

    public static Set<String> getKeyword( String... text ) {

        String temp = Strings.join( Types.toList( text ), " " );

        Set<String> keywords = new LinkedHashSet<>();
        for( String val : Strings.tokenize(temp, " \t\n:,.|;") ) {
            keywords.add( val.toLowerCase().trim() );
        }
        return keywords;
    }

}
