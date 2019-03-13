package com.nayasis.simplelauncher.controller;

import com.nayasis.simplelauncher.vo.Link;
import io.nayasis.common.cli.Command;
import io.nayasis.common.cli.CommandExecutor;
import io.nayasis.common.file.Files;
import io.nayasis.common.ui.desktop.Desktop;
import io.nayasis.common.ui.javafx.dialog.Dialog;
import lombok.extern.slf4j.Slf4j;
import io.nayasis.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
@Slf4j
public class LinkExecutor {

	@Autowired
	private MainController mainController;

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

        link = link.clone();
        link.clearBindOptions();

		CommandExecutor executor = new CommandExecutor();

		for( String commandLine : Strings.tokenize( link.getCommandPrev(), "\n" ) ) {
			run( executor, commandLine, null ).waitFor();
		}

		try {

			String execPath = getExecPathFrom( link );

			String cmd = String.format( "%s \"%s\" %s", link.getOptionPrefix(), execPath, link.getOption() );

			mainController.labelCmd.setText( cmd );

			run( executor, cmd, execPath );


		} catch( Exception e ) {
			Throwable throwable = e.getCause() == null ? e : e.getCause();
			log.error( throwable.getMessage(), throwable );
			Dialog.$.error( throwable, "msg.error.003", throwable.getMessage() );
		}

		if( ! Strings.isEmpty( link.getCommandNext() ) ) {

            final Link threadLink = link;

			new Thread( () -> {

				executor.waitFor();

				for( String commandLine : Strings.tokenize( threadLink.getCommandNext(), "\n" ) ) {
					run( executor, commandLine, null ).waitFor();
				}

			}).start();

		}

	}

	private CommandExecutor run( CommandExecutor executor, String commandLine, String workingDirectory ) {

		Command command = new Command();

		command.setWorkingDirectory( workingDirectory );

		command.set( commandLine );

		executor.run( command );

		return executor;

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

	public void openFolder( Link link ) {

		File file = new File( link.getPath() );

		if( file.isFile() ) {
			file = file.getParentFile();
		}

		if( ! file.isDirectory() ) {
			Dialog.$.error( "msg.error.005", file );
			return;
		}

		try {
			new Desktop().open( file );
		} catch( IOException e ) {
			log.error( e.getMessage(), e );
			Dialog.$.error( e, "msg.error.003", e.getMessage() );
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

}
