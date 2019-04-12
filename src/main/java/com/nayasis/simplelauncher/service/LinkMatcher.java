package com.nayasis.simplelauncher.service;

import com.nayasis.simplelauncher.vo.Link;
import io.nayasis.common.base.Strings;
import io.nayasis.common.cache.implement.LruCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

@Service
@Slf4j
public class LinkMatcher {

    private LruCache<String,List> cache = new LruCache<>( 20 );

    public boolean isKeywordMatched( List postfix, Link link ) {
        return isMatched( postfix, link, (lnk, word) -> lnk.isKeywordMatched(word) );
    }

    public boolean isGroupMatched( List postfix, Link link ) {
        return isMatched( postfix, link, (lnk, word) -> lnk.isGroupMatched(word) );
    }

    private boolean isMatched( List postfix, Link link, Matcher matcher ) {

        if( postfix.isEmpty() ) return true;

        Stack<Boolean> stack = new Stack();

        for( Object exp : postfix ) {

            if( exp instanceof Operator ) {

                Boolean v1 = stack.pop();
                Boolean v2 = stack.pop();

                switch( (Operator) exp ) {
                    case AND :
                        stack.push( v1 && v2 );
                        break;
                    case OR :
                        stack.push( v1 || v2 );
                        break;
                }

            } else {
                stack.push( matcher.isMatched( link, (String) exp ) );
            }

        }

        return stack.pop();

    }

    private interface Matcher {
        boolean isMatched( Link link, String keyword );
    }

    public List toPostfix( String text ) {

        text = text.trim();

        if( ! cache.contains(text) ) {

            List postfix = new ArrayList();

            Stack operators = new Stack();

            for ( Object exp : toExpression( text ) ) {
                if ( exp instanceof Operator ) {
                    operators.push( exp );
                } else {
                    postfix.add( exp );
                    if ( !operators.isEmpty() ) {
                        postfix.add( operators.pop() );
                    }
                }
            }

            while ( !operators.isEmpty() ) {
                postfix.add( operators.pop() );
            }

            cache.put( text, postfix );

        }

        return cache.get( text );

    }

    private List toExpression( String text ) {

        LinkedList expression = new LinkedList();

        for( String token : Strings.tokenize(text, " ,", true) ) {
            if( token.equals( " " ) ) continue;
            if( token.equals(",") ) {
                if( ! expression.isEmpty() ) {
                    if( ! (expression.peek() instanceof Operator) ) {
                        expression.push( Operator.OR );
                    }
                }
            } else {
                if( ! expression.isEmpty() ) {
                    if( ! (expression.peek() instanceof Operator) ) {
                        expression.push( Operator.AND );
                    }
                }
                expression.push( token );
            }
        }

        if( expression.peek() instanceof Operator ) {
            expression.pop();
        }

        Collections.reverse( expression );

        return expression;

    }

    private enum Operator {
        AND, OR;
    }

}
