package com.nayasis.simplelauncher.service;

import com.nayasis.simplelauncher.controller.DataController;
import com.nayasis.simplelauncher.controller.MainController;
import com.nayasis.simplelauncher.service.terminal.Terminal;
import com.nayasis.simplelauncher.service.terminal.TerminalConfig;
import com.nayasis.simplelauncher.vo.Link;
import io.nayasis.common.basica.base.Strings;
import io.nayasis.common.basica.cli.Command;
import io.nayasis.common.basica.cli.CommandExecutor;
import io.nayasis.common.basica.file.Files;
import io.nayasis.common.basicafx.desktop.Desktop;
import io.nayasis.common.basicafx.javafx.dialog.Dialog;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

@Service
@Slf4j
public class LinkExecutor {

	@Autowired
	private MainController mainController;

	@Autowired
	private DataController dataController;

	private String getExecPathFrom( Link link ) {
		if( Files.notExists( link.getPath() ) ) {
			String newPath = Files.getRootPath() + "/" + link.getRelativePath();
			if( Files.exists( newPath) ) {
				link.setPath( newPath );
			}
		}
		return link.getPath();
	}

	public void execute( Link link ) {

		if( link == null ) return;

		dataController.increaseUsedCount( link );

        link = link.clone();
        link.clearBindOptions();

		String  title       = link.getTitle().get();
		boolean showConsole = link.getShowConsole();

		try {

			for( String commandLine : Strings.tokenize( link.getCommandPrev(), "\n" ) ) {
				run( title, commandLine, null, false, true );
			}

			String execPath = getExecPathFrom( link );

			String cmd = String.format( "%s \"%s\" %s", link.getOptionPrefix(), execPath, link.getOption() );

			mainController.labelCmd.setText( cmd );

            boolean wait = Strings.isNotEmpty( link.getCommandNext() );

            run( title, cmd, execPath, showConsole, wait );

            if( wait ) {
                for( String commandLine : Strings.tokenize( link.getCommandNext(), "\n" ) ) {
                    run( title, commandLine, null, false, true );
                }
            }

		} catch( Throwable e ) {
			Throwable throwable = e.getCause() == null ? e : e.getCause();
			log.error( throwable.getMessage(), throwable );
			Dialog.error( throwable, "msg.error.003", throwable.getMessage() );
		}

	}

	public void execute( Link link, File file ) {

		Link newLink;

		if( file == null  && ! file.exists() ) {
			newLink = link;

		} else {

			newLink = link.clone();

			newLink.setbindOptions( file );

			if( file != null && file.exists() ) {
				if( link.getOption().equals(newLink.getOption()) ) {
					String newOption = newLink.getOption() + String.format( "\"%s\"", file.getPath() );
					newLink.setOption( newOption );
				}
			}

		}

		execute( newLink );

	}

	private void run( String title, String commandLine, String workingDirectory, boolean showConsole, boolean wait ) {

		Command command = new Command();
		command.setWorkingDirectory( workingDirectory );
		command.set( commandLine );

		if( showConsole ) {
			drawTerminal( title, command );
		} else {
            CommandExecutor executor = new CommandExecutor().run( command );
            if( wait ) {
                executor.waitFor();
            }
		}

	}

	public void openFolder( Link link ) {

		File file = new File( link.getPath() );

		if( file.isFile() ) {
			file = file.getParentFile();
		}

		if( ! file.isDirectory() ) {
			Dialog.error( "msg.error.005", file );
			return;
		}

		try {
			Desktop.open( file );
		} catch( Exception e ) {
			log.error( e.getMessage(), e );
			Dialog.error( e, "msg.error.003", e.getMessage() );
        }

	}

	public void copyFolder( Link link ) {

		File file = new File( link.getPath() );

		if( file.isFile() ) {
			file = file.getParentFile();
		}

		if( file.isDirectory() ) {
			new Desktop().copyToClipboard( file.getPath() );

		} else {
			new Desktop().copyToClipboard( link.getPath() );

		}

	}

	private void drawTerminal( String title, Command command ) {

		Terminal myTerminal = new Terminal( getTerminalConfig() ).setCommand( command );

		Stage stage = new Stage();
		stage.setTitle( title );
		stage.setScene( new Scene( myTerminal, 900, 600) );
		stage.show();

	}

	@NotNull
	private TerminalConfig getTerminalConfig() {
		TerminalConfig config = new TerminalConfig();
		config.setBackgroundColor( Color.rgb(16, 16, 16));
		config.setForegroundColor(Color.rgb(240, 240, 240));
		config.setCursorColor(Color.rgb(255, 0, 0, 0.5));
		config.setScrollbarVisible( false );
		config.setFontSize( 12 );
		config.setScrollWhellMoveMultiplier( 3 );
		return config;
	}

}
