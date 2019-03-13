package com.nayasis.simplelauncher.vo;

import com.nayasis.simplelauncher.common.CONSTANT;
import com.nayasis.simplelauncher.jpa.entity.LinkEntity;
import com.nayasis.simplelauncher.library.mslinks.ShellLink;
import io.nayasis.common.base.Strings;
import io.nayasis.common.etc.Platform;
import io.nayasis.common.file.Files;
import io.nayasis.common.model.NDate;
import io.nayasis.common.ui.javafx.image.Images;
import io.nayasis.common.validation.Validator;
import javafx.scene.image.Image;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Slf4j
public class Link {

    private static final long serialVersionUID = 4803934592882695337L;

	private Long          id;
	private String        title;
	private String        group;
	private String        path;
	private String        relativePath;
	private String        option;
	private String        optionPrefix;
	private String        commandPrev;
	private String        commandNext;
	private String        description;
	private String        keyword;
	private Image         icon;
	private Integer       execCount;
	private NDate         lastExecDate;

	public Link( LinkEntity entity ) {

		this.id           = entity.getId();
		this.title        = entity.getTitle();
		this.group        = entity.getGroup();
		this.path         = entity.getPath();
		this.relativePath = entity.getRelativePath();
		this.option       = entity.getOption();
		this.optionPrefix = entity.getOptionPrefix();
		this.commandPrev  = entity.getCommandPrev();
		this.commandNext  = entity.getCommandNext();
		this.description  = entity.getDescription();
		this.keyword      = entity.getKeyword();
		this.execCount    = entity.getExecCount();
		this.lastExecDate = new NDate( entity.getLastExecDate() );

		setIcon( entity.getIcon() );

	}

	public Link( File file ) {

		setTitle( Files.removeExtension( file.getName() ) );
		setPath( file );
		setIcon( file );

		if( file.isDirectory() ) {
			if( Platform.isWindows ) setOptionPrefix( "cmd /c explorer" );
		} else {
			switch( Files.getExtension(file) ) {
				case "lnk" :
					setMicrosoftLnkFile( file );
					return;
				case "jar" :
					setOptionPrefix( "java -jar" );
					break;
				default :
			}
		}

	}

	private void setMicrosoftLnkFile( File file ) {

		if( ! Platform.isWindows ) return;

		try {

			ShellLink lnk = new ShellLink( file );

			setPath( lnk.getTargetPath() );
			setOption( lnk.getCMDArgs() );
			setDescription( lnk.getName() );

			String iconPath = lnk.getIconLocation() == null ? getPath() : lnk.getIconLocation();

			setIcon( new File(iconPath) );

		} catch( IOException e ) {
			setIcon( file );
			setOptionPrefix( "cmd /c" );
        }

	}

	public boolean hasSameId( Link link ) {
		if( link == null ) return false;
		if( id == null && link.id == null ) return true;
		return id.equals( link.id );
	}

	public void setIcon( byte[] bytes ) {
		if( Validator.isEmpty(bytes) )
			bytes = CONSTANT.ICON_NEW;
		try {
			icon = Images.$.toImage( bytes );
		} catch ( UncheckedIOException e ) {
			setIcon( CONSTANT.ICON_NEW );
		}
	}

	public void setIcon( Image icon ) {
		this.icon = icon;
	}

	public boolean setIcon( File file ) {
		try {
			icon = Images.$.toImage( file );
			return true;
		} catch ( UncheckedIOException e ) {
			log.error( e.getMessage(), e );
			return false;
		}
	}

	public byte[] getIconBytes() {
		return Images.$.toBinary( icon, CONSTANT.ICON_IMAGE_TYPE );
	}

	public IconTitle getIconTitle() {
		return new IconTitle( icon, title );
	}

	public boolean isRelativePath() {
		return isRelativePath( path );
	}

	private boolean isRelativePath( String path ) {
		if( path == null ) return false;
		if( path.startsWith( "."  + File.separator ) ) return true;
		return path.startsWith( ".." + File.separator );
	}

	public Link clone() {

		Link clone = new Link();

		clone.id = id;
		clone.title = title;
		clone.group = group;
		clone.path = path;
		clone.relativePath = relativePath;
		clone.option = option;
		clone.optionPrefix = optionPrefix;
		clone.commandPrev = commandPrev;
		clone.commandNext = commandNext;
		clone.description = description;
		clone.keyword = keyword;
		clone.icon = Images.$.copy( icon );

		return clone;

	}

	public void setTitle( String title ) {
		this.title = Strings.trim( title );
	}

	public void setGroup( String group ) {
		this.group = Strings.trim( group );
	}

	public void addExecCount() {
		this.execCount ++;
		this.lastExecDate.setNow();
	}

	public void setPath( String path ) {

		path = Strings.trim( path );

		if( isRelativePath(path) ) {
			try {
				this.path = Files.toAbsolutePath( Files.getRootPath(), path );
			} catch( Exception e ) {
				log.error( e.getMessage(), e );
			}
		} else {
			this.path = path;
		}

		setRelativePath( this.path );

	}

	public void setPath( File file ) {
		if( Files.notExists(file) ) return;
		setPath( file.getAbsolutePath() );
	}

	public void setOption( String option ) {
		this.option = Strings.trim( option );
	}

	public void setOptionPrefix( String optionPrefix ) {
		this.optionPrefix = Strings.trim( optionPrefix );
	}

	public void setCommandPrev( String commandPrev ) {
		this.commandPrev = Strings.trim( commandPrev );
	}

	public void setCommandNext( String commandNext ) {
		this.commandNext = Strings.trim( commandNext );
	}

	public void setDescription( String description ) {
		this.description = Strings.trim( description );
	}

	public void setRelativePath( String relativePath ) {

		relativePath = Strings.trim( relativePath );

		if( isRelativePath(relativePath) ) {
			this.relativePath = relativePath;
		} else {
			try {
				this.relativePath = Files.toRelativePath( Files.getRootPath(), relativePath );
			} catch( Exception e ) {
				this.relativePath = relativePath;
			}
		}

	}

	public void clearId() {
		this.id           = null;
		this.lastExecDate = null;
	}

	public void setbindOptions( File file ) {
		commandNext  = setBindOptions( commandNext , file );
		commandPrev  = setBindOptions( commandPrev , file );
		option       = setBindOptions( option      , file );
		optionPrefix = setBindOptions( optionPrefix, file );
	}

	public void clearBindOptions() {
		commandNext  = clearBindOptions( commandNext  );
		commandPrev  = clearBindOptions( commandPrev  );
		option       = clearBindOptions( option       );
		optionPrefix = clearBindOptions( optionPrefix );
	}

	private String setBindOptions( String option, File file ) {

		if( option == null || file == null || ! file.exists() ) return option;

		String cd        = file.isDirectory() ? file.getPath() : file.getParent();
		String name      = file.getName();
		String unextName = file.getName().replaceFirst( "\\.[^/.]+$", "" );
		String path      = file.getPath();
		String unextPath = file.getPath().replaceFirst( "\\.[^/.]+$", "" );

		if( File.separator.equals( "\\" ) ) {
			cd        = cd.replaceAll( "\\\\", "\\\\\\\\" );
			name      = name.replaceAll( "\\\\", "\\\\\\\\" );
			unextName = unextName.replaceAll( "\\\\", "\\\\\\\\" );
			path      = path.replaceAll( "\\\\", "\\\\\\\\" );
			unextPath = unextPath.replaceAll( "\\\\", "\\\\\\\\" );
		}

		return option
			.replaceAll( "(?i)#\\{cd\\}", cd )
			.replaceAll( "(?i)#\\{name\\}", name )
			.replaceAll( "(?i)#\\{unextName\\}", unextName )
			.replaceAll( "(?i)#\\{path\\}", path )
			.replaceAll( "(?i)#\\{unextPath\\}", unextPath );

	}

	private String clearBindOptions( String option ) {
		if( option == null ) return option;
		return option
			.replaceAll( "#\\{.+?\\}", "" )
			.replaceAll( "\"\"", "" );
	}

}
