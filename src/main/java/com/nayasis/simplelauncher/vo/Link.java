package com.nayasis.simplelauncher.vo;

import com.nayasis.simplelauncher.common.CONSTANT;
import com.nayasis.simplelauncher.common.Commons;
import com.nayasis.simplelauncher.jpa.entity.LinkEntity;
import io.nayasis.basica.base.Strings;
import io.nayasis.basica.base.format.ExtractPattern;
import io.nayasis.basica.base.format.Formatter;
import io.nayasis.basica.etc.Platforms;
import io.nayasis.basica.file.Files;
import io.nayasis.basica.model.NDate;
import io.nayasis.basica.model.NMap;
import io.nayasis.basica.reflection.Reflector;
import io.nayasis.basica.validation.Validator;
import io.nayasis.basicafx.javafx.image.Images;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.image.Image;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mslinks.ShellLink;
import mslinks.ShellLinkException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Data
@NoArgsConstructor
@Slf4j
public class Link {

    private static final long serialVersionUID = 4803934592882695337L;

	private static final ExtractPattern PATTERN_SHARP  = new ExtractPattern( "#\\{(|.+?[^\\\\])\\}",  new int[] {1}, "\\\\(#|\\{|\\})",   "$1" );

	private Long                            id;
	private SimpleStringProperty            title         = new SimpleStringProperty();
	private SimpleStringProperty            group         = new SimpleStringProperty();
	private String                          path;
	private String                          relativePath;
	private Boolean                         showConsole;
	private String                          option;
	private String                          optionPrefix;
	private String                          commandPrev;
	private String                          commandNext;
	private String                          description;
	private Set<String>                     keyword;
	private SimpleObjectProperty<IconTitle> iconTitle     = new SimpleObjectProperty<>();
	private Image                           icon;
	private SimpleObjectProperty<Integer>   execCount     = new SimpleObjectProperty();
	private SimpleObjectProperty<NDate>     lastExecDate  = new SimpleObjectProperty<>();

	public Link( LinkEntity entity ) {

		this.id           = entity.getId();
		this.path         = entity.getPath();
		this.relativePath = entity.getRelativePath();
		this.showConsole  = entity.getShowConsole();
		this.option       = entity.getOption();
		this.optionPrefix = entity.getOptionPrefix();
		this.commandPrev  = entity.getCommandPrev();
		this.commandNext  = entity.getCommandNext();
		this.description  = entity.getDescription();

		this.title.set( entity.getTitle() );
		this.group.set( entity.getGrp() );
		this.execCount.set( entity.getExecCount() );

		setKeyword( new HashSet<>(Reflector.toListFrom( entity.getKeyword(), String.class )) );
		setLastExecDate( entity.getLastExecDate() );
		setIcon( entity.getIcon() );

	}

	public Link( File file ) {

		setTitle( Files.removeExtension( file.getName() ) );
		setPath( file );
		setIcon( file );

		if( file.isDirectory() ) {
			if( Platforms.isWindows ) setOptionPrefix( "cmd /c explorer" );
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

		if( ! Platforms.isWindows ) return;

		try {

			ShellLink lnk = new ShellLink( file );

			setPath( lnk.getRelativePath() );
			setOption( lnk.getCMDArgs() );
			setDescription( lnk.getName() );

			String iconPath = lnk.getIconLocation() == null ? getPath() : lnk.getIconLocation();

			setIcon( new File(iconPath) );

		} catch( IOException | ShellLinkException e ) {
			setIcon( file );
			setOptionPrefix( "cmd /c" );
        }

	}

	public void setIcon( byte[] bytes ) {
		if( Validator.isEmpty(bytes) )
			bytes = CONSTANT.ICON_NEW;
		try {
			icon = Images.toImage( bytes );
			refreshIconTitle();
		} catch ( UncheckedIOException e ) {
			setIcon( CONSTANT.ICON_NEW );
		}
	}

	public void setIcon( Image icon ) {
		this.icon = icon;
		refreshIconTitle();
	}

	public boolean setIcon( File file ) {
		try {
			icon = Images.toIconImage( file );
			refreshIconTitle();
			return true;
		} catch ( UncheckedIOException e ) {
			log.error( e.getMessage(), e );
			return false;
		}
	}

	public byte[] getIconBytes() {
		return Images.toBinary( icon, CONSTANT.ICON_IMAGE_TYPE );
	}

	private void refreshIconTitle() {
		iconTitle.set( new IconTitle( icon, title.get() ) );
	}

	public boolean isRelativePath() {
		return isRelativePath( path );
	}

	private boolean isRelativePath( String path ) {
		if( path == null ) return false;
		if( path.startsWith( "."  + File.separator ) ) return true;
		return path.startsWith( ".." + File.separator );
	}

	public Boolean getShowConsole() {
		return Validator.nvl( showConsole, Boolean.FALSE );
	}

	public boolean isShowConsole() {
		return getShowConsole();
	}

	public Link clone() {

		Link clone = new Link();

		clone.id           = id;
		clone.title        = title;
		clone.group        = group;
		clone.path         = path;
		clone.relativePath = relativePath;
		clone.showConsole  = showConsole;
		clone.option       = option;
		clone.optionPrefix = optionPrefix;
		clone.commandPrev  = commandPrev;
		clone.commandNext  = commandNext;
		clone.description  = description;
		clone.keyword      = new LinkedHashSet<>( keyword );
		clone.icon         = Images.copy( icon );

		return clone;

	}

	public void setTitle( String title ) {
		this.title.set( Strings.trim( title ) );
		refreshIconTitle();
	}

	public void setGroup( String group ) {
		this.group.set( Strings.trim( group ) );
	}

	public void setLastExecDate( NDate lastExecDate ) {
		this.lastExecDate.set( lastExecDate );
	}

	public void setLastExecDate( LocalDateTime lastExecDate ) {
		this.lastExecDate.set( lastExecDate == null ? null : new NDate( lastExecDate ) );
	}

	public void setKeyword( Set<String> keyword ) {
		this.keyword = keyword;
	}

	public boolean isKeywordMatched( Pattern pattern ) {
		if( Validator.isEmpty(pattern) || Validator.isEmpty(keyword) ) return true;
		for( String k : keyword ) {
			if( Validator.isFound(k, pattern) ) return true;
		}
		return false;
	}

	public boolean isGroupMatched( Pattern pattern ) {
		if( Validator.isEmpty(pattern) || Validator.isEmpty(group.get()) ) return true;
		return Validator.isFound( group.get().toLowerCase(), pattern );
	}

	public void setKeyword( String keyword ) {
		if( this.keyword == null ) {
			this.keyword = new HashSet<>();
		} else {
			this.keyword.clear();
		}
		this.keyword.addAll( Commons.getKeyword(keyword) );
	}

	public Link refreshKeyword() {
		setKeyword( Strings.format("{} {} {}", group.get(), title.get(), description) );
		return this;
	}

	public void addExecCount() {
		execCount.set( Validator.nvl(execCount.get(),0) + 1 );
		lastExecDate.set( new NDate() );
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
		this.id = null;
		this.lastExecDate.set( null );
	}

	public void setbindOptions( File file ) {

		Map<String,String> param = getBindingParameter( file );

		commandNext  = bindOption( commandNext , param );
		commandPrev  = bindOption( commandPrev , param );
		option       = bindOption( option      , param );
		optionPrefix = bindOption( optionPrefix, param );

	}

	private String bindOption( String option, Map<String,String> param ) {
		if( Strings.isEmpty(option) ) return option;
		return new Formatter().bindParam( PATTERN_SHARP, option, param, ( key, format, parameter ) -> Strings.nvl(parameter.get(key)) );
	}

	@NotNull
	private Map<String,String> getBindingParameter( File file ) {

		Map<String,String> param = new NMap();

		if( Files.notExists(file) ) return param;

		param.put( "filepath", file.getAbsolutePath()  );
		param.put( "dir",      file.isDirectory() ? file.getPath() : file.getParent() );
		param.put( "filename", file.getName() );
		param.put( "name",     Files.removeExtension( file.getName() ) );
		param.put( "ext",      Files.getExtension( file.getName() ) );
		param.put( "home",     System.getProperty("user.home") );

		return param;

	}

}
