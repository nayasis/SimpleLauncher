package com.nayasis.simplelauncher.controller;

import com.nayasis.simplelauncher.jpa.repository.LinkRepository;
import com.nayasis.simplelauncher.vo.Link;
import io.nayasis.common.base.Strings;
import io.nayasis.common.exception.unchecked.UncheckedIOException;
import io.nayasis.common.file.Files;
import io.nayasis.common.model.NMap;
import io.nayasis.common.reflection.Reflector;
import io.nayasis.common.ui.javafx.dialog.Dialog;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.TableView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
@Slf4j
public class DataController {

	private final String FILE_EXT_DESC = "Data File (*.sl)";
	private final String FILE_EXT      = "*.sl";

	private ObservableList<Link> linkList = FXCollections.observableArrayList();
	private SortedList<Link>     sortedList;

	private TableView<Link>      table;

	private LinkExecutor         executor = null;

	@Autowired
	private LinkRepository linkRepository;

	public DataController( SimpleLauncher ui ) {

		// Table의 Row에서 데이터를 가져오면, Call by Value 로 값이 넘어온다. (참조가 넘어오지 않는다.)
		// 그래서 Binding된 값을 Observable 하게 사용하지 못한다.

		this.table    = ui.tableMain;
		this.executor = ui.getExecutor();

		setFilter( ui );

	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
    private void setFilter( SimpleLauncher ui ) {

		FilteredList<Link> filteredList = new FilteredList<Link>( linkList, link -> true );

		ChangeListener changeListener = ( observable, oldValue, newValue ) -> {

			filteredList.setPredicate( new Predicate<Link>() {

				private String  keyword          = ui.inputKeyword.getText();
				private boolean keywordAndSearch = ui.checkboxKeywordAnd.isSelected();
				private String  group            = ui.inputGroup.getText();
				private boolean groupAndSearch   = ui.checkboxGroupAnd.isSelected();

				private Pattern patternGroup   = getRegExpPattern( group,   groupAndSearch   );
				private Pattern patternKeyword = getRegExpPattern( keyword, keywordAndSearch );

				public boolean test( Link link ) {

					if( patternGroup == null && patternKeyword == null ) return true;

					if( patternGroup   != null && ! patternGroup.matcher( link.getGroupName() ).find() ) return false;
					if( patternKeyword != null ) {
						if( ! patternKeyword.matcher( link.getKeyword() ).find() ) return false;
					}

					return true;

				}
			});

			ui.clearDetailView();
			ui.printStatus( "msg.info.005", table.getItems().size(), linkList.size() );

		};


		ui.inputKeyword.textProperty().addListener( changeListener );
		ui.inputGroup.textProperty().addListener( changeListener );
		ui.checkboxKeywordAnd.selectedProperty().addListener( changeListener );
		ui.checkboxGroupAnd.selectedProperty().addListener( changeListener );

		sortedList = new SortedList<>( filteredList );

		sortedList.comparatorProperty().bind( table.comparatorProperty() );

		table.setItems( sortedList );

	}

	public int getDataSize() {
		return linkList.size();
	}

	public void add( Link link ) {

		linkDao.insertLink( link );

		linkList.add( link );

	}

	public void delete( Link link ) {

		linkDao.deleteLink( link );

		linkList.remove( link );

	}

	public void update( Link link ) {
		linkDao.updateLink( link );
		update( link, linkList );
	}

	public void increaseLinkUsedCount( Link link ) {
		linkDao.increaseLinkUsedCount( link );
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

	private String toJson( boolean prettyPrint ) {

		List<NMap> list = new ArrayList<>();

		for( Link link :  linkList ) {

			list.add( link.toJson() );

		}

		return Reflector.toJson( list, prettyPrint );

	}

	public void exportData() {

		File file = Dialog.$.filePicker( "msg.info.003", FILE_EXT_DESC, FILE_EXT ).showSaveDialog( Main.mainStage );

		if( file == null ) return;

		try {

			Files.writeTo( file, toJson(true) );

			Dialog.$.alert( "msg.info.010", file );

		} catch( UncheckedIOException e ) {
			log.error( e.getMessage(), e );
			Dialog.$.error( e, "msg.error.003", e.getMessage() );
        }

	}

	public void importData() {

		File file = Dialog.$.filePicker( "msg.info.004", FILE_EXT_DESC, FILE_EXT ).showOpenDialog( Main.mainStage );

		if( file == null ) return;

		try {

			String jsonText = Files.readFrom( file );

			fromJson( jsonText );

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

	@SuppressWarnings( "rawtypes" )
    private void fromJson( String json ) {

        List<Map<String, Object>> convertToList = Reflector.toListFrom( json );

        Map<String, String> idConvertedMap = new HashMap<String, String>();
        List<Link>          appendedList   = new ArrayList<>();

        // Json 데이터를 테이블에 추가

		for( Map e : convertToList ) {

			Link link = new Link( e );

			String prevId = link.getId();

			linkDao.insertLink( link );
			linkList.add( link );

			idConvertedMap.put( prevId, link.getId() );
			appendedList.add( link );

		}

	}

    public void readData() {

		linkList.clear();

		for( Link link : linkDao.retrieveLinkList() ) {
			linkList.add( link );
		}

		log.debug( "bind to observable list" );

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
		return linkDao.retrieveLink( id );
	}

}
