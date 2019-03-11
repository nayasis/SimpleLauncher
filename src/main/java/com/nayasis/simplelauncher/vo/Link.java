package com.nayasis.simplelauncher.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nayasis.simplelauncher.common.CONSTANT;
import com.nayasis.simplelauncher.library.mslinks.ShellLink;
import io.nayasis.common.base.Strings;
import io.nayasis.common.base.Types;
import io.nayasis.common.etc.Platform;
import io.nayasis.common.file.Files;
import io.nayasis.common.model.NDate;
import io.nayasis.common.model.NMap;
import io.nayasis.common.reflection.Reflector;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;

@Data
@NoArgsConstructor
@Slf4j
public class Link {

    private static final long serialVersionUID = 4803934592882695337L;

    private String  id;
	private String  title;
	private String  groupName;
	private int     execCount;
	private String  lastUsedDt;
	private String  execPath;
	private String  execPathRelative;
	private String  execOption;
	private String  execOptionPrefix;
	private String  cmdPrev;
	private String  cmdNext;
	private String  description;
	private Image   icon;

	private final static String ICON_IMAGE_TYPE = "png";

	public Link( File file ) {

		setTitle( Files.removeExtension( file.getName() ) );

		setExecPath( file.getAbsolutePath() );

		if( file.isDirectory() ) {
			if( Platform.isWindows ) setExecOptionPrefix( "cmd /c explorer" );
		} else {

			switch( Files.getExtension(file) ) {
				case "lnk" :
					setMicrosoftLnkFile( file );
					return;
				case "jar" :
					setExecOptionPrefix( "java -jar" );
					break;
				default :
			}

		}

		setIcon( file );

	}

	private void setMicrosoftLnkFile( File file ) {

		if( ! Platform.isWindows ) return;

		try {

			ShellLink lnk = new ShellLink( file );

			setExecPath( lnk.getTargetPath() );
			setExecOption( lnk.getCMDArgs() );
			setDescription( lnk.getName() );

			String iconPath = lnk.getIconLocation() == null ? getExecPath() : lnk.getIconLocation();

			setIcon( new File(iconPath) );

		} catch( IOException e ) {

			log.error( e.getMessage(), e );

			setIcon( file );
			setExecOptionPrefix( "cmd /c" );

        }

	}

	public Link( Map map ) {

		if( map instanceof NMap ) {
			from( (NMap) map );
		} else {
			from( new NMap(map) );
		}

	}

	public boolean hasSameId( Link link ) {

		if( link == null ) return false;

		if( id == null && link.id == null ) return true;

		return id.equals( link.id );

	}

	public void setIcon( String iconString ) {

        try {
        	setIcon( (byte[]) Strings.decode( iconString ) );
        } catch( ClassCastException e ) {
        	log.error( e.getMessage(), e );
        }

	}

	public void setIcon( byte[] iconByte ) {

		if( iconByte == null || iconByte.length == 0 ) {
			iconByte = CONSTANT.ICON_NEW;
		}

		try {

			ByteArrayInputStream stream = new ByteArrayInputStream( iconByte );

			BufferedImage image = ImageIO.read( stream );

			if( image == null ) return;

			icon = SwingFXUtils.toFXImage( image, null );

		} catch( IOException e ) {
			log.error( e.getMessage(), e );
			setIcon( CONSTANT.ICON_NEW );
		}

	}

	@JsonIgnore
	public void setIcon( Image icon ) {
		this.icon = icon;
	}

	@JsonIgnore
	public boolean setIcon( File file ) {

		if( file == null || ! file.exists() ) return false;

		if( file.canExecute() ) {

			ImageIcon readIcon = (ImageIcon) FileSystemView.getFileSystemView().getSystemIcon( file );

			try {

				icon = SwingFXUtils.toFXImage( (BufferedImage) readIcon.getImage(), null );

			} catch( ClassCastException e ) {
				log.error( e.getMessage(), e );
				setIcon( CONSTANT.ICON_NEW );
			}

		} else {
			icon = new Image( file.getAbsolutePath() );
		}

		return true;

	}

	public String getIconString() {
		return Strings.encode( getIconByte() );
	}

	public byte[] getIconByte() {

		try {

			ByteArrayOutputStream stream = new ByteArrayOutputStream();

			ImageIO.write( SwingFXUtils.fromFXImage(icon, null), ICON_IMAGE_TYPE, stream );

			return stream.toByteArray();

		} catch( NullPointerException | IOException e ) {
			log.error( e.getMessage(), e );
	        return new byte[] {};
        }


	}

	@JsonIgnore
	public Image getIcon() {
		return icon;
	}

	public String getKeyword() {
		return Strings.nvl( title ) + " :: " + Strings.nvl( groupName );
	}

	@JsonIgnore
	public IconTitle getIconTitle() {
		return new IconTitle( icon, title );
	}

	public boolean isRelativePath() {
		return isRelativePath( execPath );
	}

	private boolean isRelativePath( String path ) {

		if( path == null ) return false;

		if( path.startsWith( "."  + File.separator ) ) return true;
		return path.startsWith( ".." + File.separator );

	}

	public Link clone() {
		return Reflector.clone( this );
	}

	public String getId() {
		return id;
	}

	public void setId( String id ) {
		this.id = id;
	}

	public void initId() {

		id = null;

		setLastUsedDt( "" );
	}

	public String getTitle() {
		return title;
	}

	public void setTitle( String title ) {
		this.title = Strings.nvl( title ).trim();
	}

	public String getGroupName() {
		return Strings.nvl( groupName );
	}

	public void setGroupName( String groupName ) {
		this.groupName = Strings.nvl( groupName ).trim();
	}

	public int getExecCount() {
		return execCount;
	}

	public void setExecCount( int execCount ) {
		this.execCount = execCount;
	}

	public void addExecCount() {
		this.execCount ++;
		setLastUsedDt( new NDate().toString( "YYYY-MM-DD HH:MI" ) );
	}

	public String getLastUsedDt() {
		return lastUsedDt;
	}

	public void setLastUsedDt( String lastUsedDt ) {
		this.lastUsedDt = lastUsedDt;
	}

	public String getExecPath() {
		return execPath;
	}

	public void setExecPath( String execPath ) {

		execPath = Strings.nvl( execPath ).trim();

		this.execPath = execPath;

		if( isRelativePath(execPath) ) {

			try {
				this.execPath = Files.toAbsolutePath( Files.getRootPath(), execPath );
			} catch( Exception e ) {
//				NLogger.warn( e );
			}

		}

		if( Strings.isEmpty( this.execPathRelative ) ) {
			setExecPathRelative( execPath );
		}

	}

	public void setExecPath( File file ) {

		if( file == null ) return;

		setExecPath( file.getAbsolutePath() );

	}

	public String getExecOption() {
		return Strings.nvl( execOption ).trim();
	}

	public void setExecOption( String execOption ) {
		this.execOption = Strings.nvl( execOption ).trim();
	}

	public String getCmdPrev() {
		return cmdPrev;
	}

	public void setCmdPrev( String cmdPrev ) {
		this.cmdPrev = Strings.nvl( cmdPrev ).trim();
	}

	public String getCmdNext() {
		return cmdNext;
	}

	public void setCmdNext( String cmdNext ) {
		this.cmdNext = Strings.nvl( cmdNext ).trim();
	}

	public String getDescription() {
		return Strings.nvl( description );
	}

	public void setDescription( String description ) {
		this.description = Strings.nvl( description ).trim();
	}

	public String toString() {
		return Reflector.toString( this );
	}

	public NMap toJson() {

		NMap row = new NMap( this );

		row.remove( "icon"         );
		row.remove( "iconTitle"    );
		row.remove( "iconByte"     );
		row.remove( "relativePath" );

		return row;

	}

	private Link from( NMap row ) {

		setId( Types.toString(row.get(  "id"          )) );
		setTitle(       Types.toString(row.get(  "title"       ) ));
		setGroupName(   Types.toString(row.get(  "groupName"   ) ));
		setExecCount(   Types.toInt(row.get(     "execCount"   )));
		setLastUsedDt(  Types.toString(row.get(  "lastUsedDt"  ) ));
		setExecPath(    Types.toString(row.get(  "execPath"    ) ));
		setExecOption( Types.toString(row.get( "execOption" ) ));
		setExecOptionPrefix( Types.toString(row.get( "execOptionPrefix" ) ));
		setCmdPrev( Types.toString(row.get( "cmdPrev" ) ));
		setDescription( Types.toString(row.get( "description" ) ));

		try {

			if( row.containsKey( "iconByte" ) ) {
				setIcon( (byte[]) row.get("iconByte") );
			} else if( row.containsKey( "iconString" ) ) {
				setIcon( Types.toString( row.get( "iconString" )) );
			}

		} catch( Exception e ) {
			log.error( e.getMessage(), e );
		}

		return this;

	}

	public String getExecOptionPrefix() {
	    return execOptionPrefix;
    }

	public void setExecOptionPrefix( String execOptionPrefix ) {
	    this.execOptionPrefix = Strings.nvl( execOptionPrefix );
    }

	public String getExecPathRelative() {
	    return execPathRelative;
    }

	public void setExecPathRelative( String execPathRelative ) {

		execPathRelative = Strings.nvl( execPathRelative ).trim();

		if( isRelativePath(execPathRelative) ) {
			this.execPathRelative = execPathRelative;

		} else {

			try {
				this.execPathRelative = Files.toRelativePath( Files.getRootPath(), execPathRelative );
			} catch( Exception e ) {
				this.execPathRelative = execPathRelative;
			}

		}

	}

	public void setbindOptions( File file ) {
		cmdNext          = setBindOptions( cmdNext,          file );
		cmdPrev          = setBindOptions( cmdPrev,          file );
		execOption       = setBindOptions( execOption,       file );
		execOptionPrefix = setBindOptions( execOptionPrefix, file );
	}

	public void clearBindOptions() {
		cmdNext          = clearBindOptions( cmdNext          );
		cmdPrev          = clearBindOptions( cmdPrev          );
		execOption       = clearBindOptions( execOption       );
		execOptionPrefix = clearBindOptions( execOptionPrefix );
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
