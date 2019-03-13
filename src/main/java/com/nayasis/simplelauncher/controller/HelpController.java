package com.nayasis.simplelauncher.controller;

import io.nayasis.common.base.Strings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Component
@Slf4j
public class HelpController {

    @FXML private Hyperlink linkMailTo;

    @FXML private Text desc;
	
    @FXML
    public void clickLinkMailTo( ActionEvent event ) {

    	log.debug( event.toString() );

    	System.setProperty( "java.awt.headless", "true" );

		try {
			Desktop.getDesktop().browse( getMailLink() );
		} catch( IOException | URISyntaxException e ) {
			log.error( e.getMessage(), e );
		}

//		new Thread(() -> {
//			try {
//				Desktop.getDesktop().browse( getMailLink() );
//			} catch( IOException | URISyntaxException e ) {
//				log.error( e.getMessage(), e );
//			}
//		}).start();

    }

	private URI getMailLink() throws URISyntaxException {
		
		String title = Strings.encodeUrl( "[SimpleLauncher] Ask some question" );
		
	    return new URI( String.format("mailto:%s?subject=%s", "nayasis@gmail.com", title) );
    }
    


}
