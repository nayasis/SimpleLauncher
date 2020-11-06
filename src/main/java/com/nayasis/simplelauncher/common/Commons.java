package com.nayasis.simplelauncher.common;

import com.github.nayasis.basica.base.Strings;
import com.github.nayasis.basica.base.Types;

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
