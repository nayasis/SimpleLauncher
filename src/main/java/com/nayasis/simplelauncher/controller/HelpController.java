package com.nayasis.simplelauncher.controller;

import com.github.nayasis.basica.base.Strings;
import com.github.nayasis.basicafx.desktop.Desktop;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HelpController {

    @FXML
    public void sendMail( ActionEvent event ) {
    	Desktop.browse( getMailLink() );
    }

	private String getMailLink() {
		String title = Strings.encodeUrl( "[SimpleLauncher] Ask some question" );
	    return String.format("mailto:%s?subject=%s", "nayasis@gmail.com", title );
    }

}
