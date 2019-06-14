package com.nayasis.simplelauncher.controller;

import com.nayasis.simplelauncher.jpa.entity.LinkEntity;
import com.nayasis.simplelauncher.jpa.repository.LinkRepository;
import com.nayasis.simplelauncher.vo.JsonLink;
import com.nayasis.simplelauncher.vo.Link;
import com.nayasis.simplelauncher.vo.OldJsonLink;
import io.nayasis.common.basica.exception.unchecked.UncheckedIOException;
import io.nayasis.common.basica.file.Files;
import io.nayasis.common.basica.model.NDate;
import io.nayasis.common.basica.reflection.Reflector;
import io.nayasis.common.basicafx.javafx.dialog.Dialog;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.nayasis.simplelauncher.common.CONSTANT.STAGE.MAIN;

@Component
@Slf4j
public class DataController {

	private final String FILE_EXT_DESC = "Data File (*.sl)";
	private final String FILE_EXT      = "*.sl";

	@Autowired
	private LinkRepository linkRepository;

	@Autowired
	private MainController main;

	private LinkEntity toLinkEntity( Link link ) {

		LinkEntity entity = getEntity( link.getId() );
		if( entity == null ) {
			entity = new LinkEntity();
		}

		entity.setId( link.getId() );
		entity.setTitle( link.getTitle().get() );
		entity.setGrp( link.getGroup().get() );
		entity.setPath( link.getPath() );
		entity.setRelativePath( link.getRelativePath() );
		entity.setShowConsole( link.getShowConsole() );
		entity.setOption( link.getOption() );
		entity.setOptionPrefix( link.getOptionPrefix() );
		entity.setCommandNext( link.getCommandNext() );
		entity.setCommandPrev( link.getCommandPrev() );
		entity.setDescription( link.getDescription() );
		entity.setKeyword( link.getKeyword() );
		entity.setIcon( link.getIconBytes() );
		entity.setExecCount( link.getExecCount().get() );
		entity.setLastExecDate( link.getLastExecDate().get() );

		return entity;

	}

	private LinkEntity getEntity( Long id ) {
		if( id == null ) return null;
		Optional<LinkEntity> entity = linkRepository.findById( id );
		return entity.isPresent() ? entity.get() : null;
	}

	@Transactional
	public void add( Link link ) {
		LinkEntity entity = toLinkEntity( link );
		linkRepository.save( entity );
		link.setId( entity.getId() );
		main.tableMain.add( link );
	}

	@Transactional
	public void delete( Link link ) {
		linkRepository.deleteById( link.getId() );
		main.tableMain.remove( link );
	}

	public void update( Link link ) {
		LinkEntity entity = toLinkEntity( link );
		linkRepository.save( entity );
	}

	@Transactional
	public void increaseUsedCount( Link link ) {
		LinkEntity entity = getEntity( link.getId() );
		if( entity == null ) return;
		link.addExecCount();
		link.setLastExecDate( new NDate() );
		entity.setLastExecDate( link.getLastExecDate().get() );
		entity.setExecCount( link.getExecCount().get() );
		linkRepository.save( entity );
	}

	@Transactional
	public void updateExecPath( Link link ) {

		LinkEntity entity = getEntity( link.getId() );
		if( entity == null ) return;

		entity.setPath( link.getPath() );
		linkRepository.save( entity );

	}

	public void exportData() {

		File file = Dialog.filePicker( "msg.info.003", FILE_EXT_DESC, FILE_EXT ).showSaveDialog( MAIN );

		if( file == null ) return;

		try {

			List<JsonLink> links = new ArrayList<>();

			linkRepository.findAll( Sort.by( "id" ) ).forEach( link -> {
				links.add( new JsonLink( link ) );
			} );

			Files.writeTo( file, Reflector.toJson( links, true ) );

			Dialog.alert( "msg.info.010", file );

		} catch( UncheckedIOException e ) {
			log.error( e.getMessage(), e );
			Dialog.error( e, "msg.error.003", e.getMessage() );
        }

	}

	@Transactional
	public void importData() {

		File file = Dialog.filePicker( "msg.info.004", FILE_EXT_DESC, FILE_EXT ).showOpenDialog( MAIN );

		if( file == null ) return;

		try {

			String json = Files.readFrom( file );

			List<LinkEntity> entities = new ArrayList<>();

			boolean importOldLink = false;
			try {
				List<OldJsonLink> oldLinks = Reflector.toListFrom( json, OldJsonLink.class );
				if( ! oldLinks.isEmpty() && ! oldLinks.get(0).getId().isEmpty() ) {
					oldLinks.forEach( e -> entities.add( e.toLinkEntity() ) );
					importOldLink = true;
				}
			} catch ( Exception e ) {}

			if( ! importOldLink ) {
				List<JsonLink> jsonLinks = Reflector.toListFrom( json, JsonLink.class );
				jsonLinks.forEach( e -> entities.add( e.toLinkEntity() ) );
			}

			linkRepository.saveAll( entities );

			Platform.runLater( () -> readData() );

			Dialog.alert( "msg.info.009", file );

		} catch( Exception e ) {
			log.error( e.getMessage(), e );
			Dialog.error( e, "msg.error.003", e.getMessage() );
        }

	}

	@Transactional
	public void clearData() {
		if( ! Dialog.confirm( "msg.confirm.003" ) ) return;
		linkRepository.deleteAll();
		main.tableMain.clear();
		main.clearDetailView();
	}

    public void readData() {
		main.tableMain.clear();
		List<LinkEntity> links = linkRepository.findAllByOrderByGrpAscTitleAsc();
		log.debug( ">> links : {}", links.size() );
		links.forEach(entity -> {
            Link link = new Link(entity);
			main.tableMain.getData().add(link);
		});
	}

}
