package com.nayasis.simplelauncher.service;

import com.github.nayasis.basica.base.Strings;
import com.github.nayasis.basica.cli.Command;
import com.github.nayasis.basica.cli.CommandExecutor;
import com.github.nayasis.basica.file.Files;
import com.github.nayasis.basicafx.desktop.Desktop;
import com.github.nayasis.basicafx.javafx.dialog.Dialog;
import com.github.nayasis.basicafx.javafx.stage.ConfigurableStage;
import com.nayasis.simplelauncher.controller.DataController;
import com.nayasis.simplelauncher.controller.MainController;
import com.nayasis.simplelauncher.service.terminal.Terminal;
import com.nayasis.simplelauncher.service.terminal.TerminalConfig;
import com.nayasis.simplelauncher.vo.Link;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@Slf4j
public class LinkExecutor {

	@Autowired
	private MainController mainController;

	@Autowired
	private DataController dataController;

	private String getExecPathFrom( Link link ) {

		String path = link.getPath();

		if( Files.exists(path) )
			return path;

		path = Files.rootPath() + "/" + link.getPath();

		if( Files.exists(path) )
			return path;

		path = Files.rootPath() + "/" + link.getRelativePath();

		if( Files.exists(path) ) {
			link.setPath( path );
			dataController.updateExecPath( link );
			return path;
		}

		return link.getPath();

	}

	private String wrapDoubleQuotation( String path ) {
		return Strings.format( "\"{}\"", path );
	}

	public void execute( Link link, File file ) {

		Link execLink;

		if( file == null || ! file.exists() ) {
			execLink = link;

		} else {

			execLink = link.clone();
			execLink.setbindOptions( file );

			// if option not changed after binding, just add file path as parameter.
			if( link.getOption().equals(execLink.getOption()) ) {
				execLink.setOption( Strings.format( "{} \"{}\"", execLink.getOption(), file.getPath() ) );
			}

		}

		execute( execLink );

	}

	public void execute( Link link ) {

		if( link == null ) return;

		dataController.increaseUsedCount( link );

		try {

			run( link.getCommandPrev() );

			String execPath = getExecPathFrom( link );
			String cmd      = getLinkCommand( execPath, link );

			Command command = new Command();
			command.set( cmd );

			mainController.printCommand( cmd );

			if( Files.isFile(execPath) ) {
				command.setWorkingDirectory( Files.directory(execPath).toFile() );
			}

			if( link.isShowConsole() ) {
				drawTerminal( link.getTitle().get(), command, getPostAction(link) );
			} else {
				CommandExecutor executor = new CommandExecutor().run( command );
				if( Strings.isNotEmpty(link.getCommandNext()) ) {
					executor.waitFor();
					run( link.getCommandNext() );
				}
			}

		} catch( Throwable e ) {
			Throwable throwable = e.getCause() == null ? e : e.getCause();
			log.error( throwable.getMessage(), throwable );
			Dialog.error( throwable, "msg.error.003", throwable.getMessage() );
		}

	}

	private void run( String commandLines ) {
		for( String commandLine : Strings.tokenize( commandLines, "\n" ) ) {
			run( commandLine,  true );
		}
	}

	@Nullable
	private Runnable getPostAction( Link link ) {
		Runnable postAction = null;
		String commandNext = link.getCommandNext();
		if( Strings.isNotEmpty(commandNext) ) {
			postAction = () -> {
				try {
					run( link.getCommandNext() );
				} catch ( Throwable e ) {
					Platform.runLater( () -> {
						Dialog.error( e, e.getMessage() );
					});
				}
			};
		}
		return postAction;
	}

	private String getLinkCommand( String execPath, Link link ) {

		StringBuilder cmd = new StringBuilder();

		if( Strings.isNotEmpty( link.getOptionPrefix() ) )
			cmd.append( link.getOptionPrefix() ).append( " " );

		if( Files.isFile(execPath) ) {
			cmd.append( wrapDoubleQuotation( execPath ) );
		} else {
			cmd.append( execPath );
		}

		if( Strings.isNotEmpty( link.getOption() ) )
			cmd.append( " " ).append( link.getOption() );

		return cmd.toString();

	}

	private void run( String commandLine, boolean wait ) {

		Command command = new Command();
		command.set( commandLine );

		if( Files.isFile(commandLine) )
			command.setWorkingDirectory( Files.directory(commandLine).toFile() );

		CommandExecutor executor = new CommandExecutor().run( command );
		if( wait )
			executor.waitFor();

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

	private Terminal drawTerminal( String title, Command command, Runnable runnable ) {

		Stage stage = new ConfigurableStage();

		Terminal terminal = new Terminal( getTerminalConfig() ).setCommand( command ).setStage( stage ).setPostAction( runnable );

		stage.setTitle( title );
		stage.setScene( new Scene( terminal, 900, 600) );
		stage.show();

		return terminal;

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
