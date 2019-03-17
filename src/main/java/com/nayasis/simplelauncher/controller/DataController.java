package com.nayasis.simplelauncher.controller;

import com.nayasis.simplelauncher.jpa.entity.LinkEntity;
import com.nayasis.simplelauncher.jpa.repository.LinkRepository;
import com.nayasis.simplelauncher.vo.JsonLink;
import com.nayasis.simplelauncher.vo.Link;
import io.nayasis.common.exception.unchecked.UncheckedIOException;
import io.nayasis.common.file.Files;
import io.nayasis.common.reflection.Reflector;
import io.nayasis.common.ui.javafx.control.table.NTable;
import io.nayasis.common.ui.javafx.dialog.Dialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
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

	private ObservableList<Link> linkList = FXCollections.observableArrayList();
	private SortedList<Link>     sortedList;

	private NTable<Link> table;

	@Autowired
	private LinkRepository linkRepository;

	@Autowired
	private MainController main;

	@Autowired
	private LinkExecutor executor;

    public DataController init() {
        // Table의 Row에서 데이터를 가져오면, Call by Value 로 값이 넘어온다. (참조가 넘어오지 않는다.)
        // 그래서 Binding된 값을 Observable 하게 사용하지 못한다.
        this.table = main.tableMain;
//        setFilter();
        return this;
    }

//    public void setFilter() {
//
//		FilteredList<Link> filteredList = new FilteredList<>( linkList, link -> true );
//
//		ChangeListener changeListener = new ChangeListener() {
//            @Override
//            public void changed( ObservableValue observable, Object oldValue, Object newValue ) {
//
//                filteredList.setPredicate(new Predicate<Link>() {
//
//                    private String keyword = main.inputKeyword.getText();
//                    private boolean keywordAndSearch = main.checkboxKeywordAnd.isSelected();
//                    private String group = main.inputGroup.getText();
//                    private boolean groupAndSearch = main.checkboxGroupAnd.isSelected();
//
//                    private Pattern patternGroup = getRegExpPattern(group, groupAndSearch);
//                    private Pattern patternKeyword = getRegExpPattern(keyword, keywordAndSearch);
//
//                    public boolean test( Link link ) {
//
//                        if (patternGroup == null && patternKeyword == null) return true;
//
//                        if (patternGroup != null && !patternGroup.matcher(link.getGroup()).find()) return false;
//                        if (patternKeyword != null) {
//                            if (!patternKeyword.matcher(link.getKeyword()).find()) return false;
//                        }
//
//                        return true;
//
//                    }
//                });
//
//                main.clearDetailView();
//                main.printStatus("msg.info.005", table.getData().size(), linkList.size());
//
//            }
//        };
//
//
//		main.inputKeyword.textProperty().addListener( changeListener );
//		main.inputGroup.textProperty().addListener( changeListener );
//		main.checkboxKeywordAnd.selectedProperty().addListener( changeListener );
//		main.checkboxGroupAnd.selectedProperty().addListener( changeListener );
//
//		sortedList = new SortedList<>( filteredList );
//
//		sortedList.comparatorProperty().bind( table.comparatorProperty() );
//
////		table.setData( sortedList );
//
//	}

//	public int getDataSize() {
//		return linkList.size();
//	}

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
		table.add( link );
	}

	@Transactional
	public void delete( Link link ) {
		linkRepository.deleteById( link.getId() );
		table.remove( link );
	}

	public void update( Link link ) {
		LinkEntity entity = toLinkEntity( link );
		linkRepository.save( entity );
		update( link, linkList );
	}

	@Transactional
	public void increaseLinkUsedCount( Link link ) {
		LinkEntity entity = getEntity( link.getId() );
		if( entity == null ) return;
		link.addExecCount();
		entity.setLastExecDate( link.getLastExecDate().get() );
		entity.setExecCount( link.getExecCount().get() );
		linkRepository.save( entity );
//		update( link, linkList );
	}

	private void update( Link link, List<Link> list ) {

        return;

//		Integer index = getIndexInList( link, list );
//
//		if( index == null ) return;
//
//		list.set( index, link );
//
//		table.getSelectionModel().clearSelection();
//		table.getSelectionModel().select( link );

	}

	private Integer getIndexInList( Link link, List<Link> list ) {
		for( int i = 0, iCnt = list.size(); i < iCnt; i++ ) {
			if( list.get(i).hasSameId(link) ) {
				return i;
			}
		}
		return null;
	}

	public void exportData() {

		File file = Dialog.$.filePicker( "msg.info.003", FILE_EXT_DESC, FILE_EXT ).showSaveDialog( MAIN );

		if( file == null ) return;

		try {

			List<JsonLink> links = new ArrayList<>();

			linkRepository.findAll( Sort.by( "id" ) ).forEach( link -> {
				links.add( new JsonLink( link ) );
			} );

			Files.writeTo( file, Reflector.toJson( links, true ) );

			Dialog.$.alert( "msg.info.010", file );

		} catch( UncheckedIOException e ) {
			log.error( e.getMessage(), e );
			Dialog.$.error( e, "msg.error.003", e.getMessage() );
        }

	}

	@Transactional
	public void importData() {

		File file = Dialog.$.filePicker( "msg.info.004", FILE_EXT_DESC, FILE_EXT ).showOpenDialog( MAIN );

		if( file == null ) return;

		try {

			String json = Files.readFrom( file );

			List<LinkEntity> entities = new ArrayList<>();

			Reflector.toListFrom( json, JsonLink.class ).forEach( e -> entities.add( e.toLinkEntity() ) );

			linkRepository.saveAll( entities );

			entities.forEach( entity -> {
				linkList.add( new Link(entity) );
			});

			Dialog.$.alert( "msg.info.009", file );

		} catch( UncheckedIOException e ) {
			log.error( e.getMessage(), e );
			Dialog.$.error( e, "msg.error.003", e.getMessage() );
        }

	}

	public void clearData() {
		if( ! Dialog.$.confirm( "msg.confirm.003" ) ) return;
		linkList.clear();
	}

    public void readData() {
		linkList.clear();
		List<LinkEntity> links = linkRepository.findAll();
		log.debug( ">> links : {}", links.size() );
		links.forEach(entity -> {
            Link link = new Link(entity);
            table.getData().add(link);
//			linkList.add(link);
		});

//		table.bind( linkList );

	}

	public void executeLink() {
		Link link = table.getFocusedItem();
		executeLink( link );
	}

	public void executeLink( Link link ) {
		if( link == null ) return;
		increaseLinkUsedCount( link );
		executor.execute( link );
	}

	public void executeLink( Link link, File fileDragged ) {
		if( link == null ) return;
		increaseLinkUsedCount( link );
		executor.execute( link, fileDragged );
	}

	public Link getLink( Long id ) {
		return new Link( getEntity(id) );
	}

}
