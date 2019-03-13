package com.nayasis.simplelauncher.controller;

import com.nayasis.simplelauncher.jpa.entity.LinkEntity;
import com.nayasis.simplelauncher.jpa.repository.LinkRepository;
import com.nayasis.simplelauncher.vo.JsonLink;
import com.nayasis.simplelauncher.vo.Link;
import io.nayasis.common.base.Strings;
import io.nayasis.common.exception.unchecked.UncheckedIOException;
import io.nayasis.common.file.Files;
import io.nayasis.common.reflection.Reflector;
import io.nayasis.common.ui.javafx.dialog.Dialog;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.Initializable;
import javafx.scene.control.TableView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.nayasis.simplelauncher.common.CONSTANT.STAGE.MAIN;

@Component
@Slf4j
public class DataController implements Initializable {

	private final String FILE_EXT_DESC = "Data File (*.sl)";
	private final String FILE_EXT      = "*.sl";

	private ObservableList<Link> linkList = FXCollections.observableArrayList();
	private SortedList<Link>     sortedList;

	private TableView<Link> table;

	@Autowired
	private LinkRepository linkRepository;

	@Autowired
	private MainController mainController;

	@Autowired
	private LinkExecutor executor;

	@Override
	public void initialize( URL location, ResourceBundle resources ) {

		// Table의 Row에서 데이터를 가져오면, Call by Value 로 값이 넘어온다. (참조가 넘어오지 않는다.)
		// 그래서 Binding된 값을 Observable 하게 사용하지 못한다.

		this.table = mainController.tableMain;

		setFilter();

	}

    private void setFilter() {

		FilteredList<Link> filteredList = new FilteredList<Link>( linkList, link -> true );

		ChangeListener changeListener = ( observable, oldValue, newValue ) -> {

			filteredList.setPredicate( new Predicate<Link>() {

				private String  keyword          = mainController.inputKeyword.getText();
				private boolean keywordAndSearch = mainController.checkboxKeywordAnd.isSelected();
				private String  group            = mainController.inputGroup.getText();
				private boolean groupAndSearch   = mainController.checkboxGroupAnd.isSelected();

				private Pattern patternGroup   = getRegExpPattern( group,   groupAndSearch   );
				private Pattern patternKeyword = getRegExpPattern( keyword, keywordAndSearch );

				public boolean test( Link link ) {

					if( patternGroup == null && patternKeyword == null ) return true;

					if( patternGroup   != null && ! patternGroup.matcher( link.getGroup() ).find() ) return false;
					if( patternKeyword != null ) {
						if( ! patternKeyword.matcher( link.getKeyword() ).find() ) return false;
					}

					return true;

				}
			});

			mainController.clearDetailView();
			mainController.printStatus( "msg.info.005", table.getItems().size(), linkList.size() );

		};


		mainController.inputKeyword.textProperty().addListener( changeListener );
		mainController.inputGroup.textProperty().addListener( changeListener );
		mainController.checkboxKeywordAnd.selectedProperty().addListener( changeListener );
		mainController.checkboxGroupAnd.selectedProperty().addListener( changeListener );

		sortedList = new SortedList<>( filteredList );

		sortedList.comparatorProperty().bind( table.comparatorProperty() );

		table.setItems( sortedList );

	}

	public int getDataSize() {
		return linkList.size();
	}

	private LinkEntity toLinkEntity( Link link ) {

		LinkEntity entity = getEntity( link.getId() );
		if( entity == null ) {
			entity = new LinkEntity();
		}

		entity.setId( link.getId() );
		entity.setTitle( link.getTitle() );
		entity.setGrp( link.getGroup() );
		entity.setPath( link.getPath() );
		entity.setRelativePath( link.getRelativePath() );
		entity.setOption( link.getOption() );
		entity.setOptionPrefix( link.getOptionPrefix() );
		entity.setCommandNext( link.getCommandNext() );
		entity.setCommandPrev( link.getCommandPrev() );
		entity.setDescription( link.getDescription() );
		entity.setKeyword( link.getKeyword() );
		entity.setIcon( link.getIconBytes() );
		entity.setExecCount( link.getExecCount() );
		entity.setLastExecDate( link.getLastExecDate() );

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
		linkList.add( link );
	}

	@Transactional
	public void delete( Link link ) {
		linkRepository.deleteById( link.getId() );
		linkList.remove( link );
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
		update( link, linkList );
	}

	private void update( Link link, List<Link> list ) {

		Integer index = getIndexInList( link, list );

		if( index == null ) return;

		list.set( index, link );

		table.getSelectionModel().clearSelection();
		table.getSelectionModel().select( link );

	}

	private Integer getIndexInList( Link link, List<Link> list ) {
		for( int i = 0, iCnt = list.size(); i < iCnt; i++ ) {
			if( list.get(i).hasSameId(link) ) {
				return i;
			}
		}
		return null;
	}

	private Pattern getRegExpPattern( String text, boolean isAnd ) {

		if( text == null ) return null;

		text = Strings.compressSpace( text ).trim();

		if( Strings.isEmpty(text) ) return null;

		try {

			text = text
					.replaceAll( "([\\^\\$\\+\\*\\?\\.\\{\\}\\[\\]\\|])", "\\$1" )
					.replaceAll( "\\*", ".*?" )
					;

			StringBuilder sb = new StringBuilder();

			sb.append( "(?mis)" );

			List<String> split = Strings.tokenize( text, " " );

			if( isAnd ) {

				sb.append( "(?=.*" );
				sb.append( Strings.join( split, ")(?=.*" ) );
				sb.append( ")" );

			} else {

				sb.append( "(" );
				sb.append( Strings.join( split, "|" ) );
				sb.append( ")" );

			}

			return Pattern.compile( sb.toString() );


		} catch( PatternSyntaxException e ) {

			log.error( Strings.format("Error in parsing pattern : {}", text), e );

			throw e;

		}

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
			linkList.add( new Link(entity) );
		});
	}

	public void executeLink() {
		Link link = table.getFocusModel().getFocusedItem();
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
