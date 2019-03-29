package com.nayasis.simplelauncher.common;

import io.nayasis.common.ui.javafx.dialog.Dialog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class FxmlErrorHandler {

//    @Around( "@annotation(javafx.fxml.FXML)")
//    @Around( "execution(* com.nayasis.simplelauncher.controller.*.*(..))")
    public Object showError( ProceedingJoinPoint joinPoint ) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch ( Throwable e ) {
            log.error( e.getMessage(), e );
            Dialog.error( e, e.getMessage() );
            throw e;
        }
    }

}
