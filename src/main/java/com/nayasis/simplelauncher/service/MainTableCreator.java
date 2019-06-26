package com.nayasis.simplelauncher.service;

import com.nayasis.simplelauncher.controller.MainController;
import com.nayasis.simplelauncher.vo.IconTitle;
import com.nayasis.simplelauncher.vo.Link;
import io.nayasis.common.basica.base.Strings;
import io.nayasis.common.basica.model.NDate;
import io.nayasis.common.basicafx.javafx.control.table.NTable;
import io.nayasis.common.basicafx.javafx.control.table.NTableColumn;
import io.nayasis.common.basicafx.javafx.control.table.byfunction.CellFormatter;
import io.nayasis.common.basicafx.javafx.control.table.byfunction.TableViewDataFilter;
import io.nayasis.common.basicafx.javafx.etc.FxThread;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.function.Predicate;

import static com.nayasis.simplelauncher.common.CONSTANT.KEYPRESS_BLOCK_WAIT_MILISEC;
import static javafx.scene.input.KeyCode.UNDEFINED;

@Service
@Slf4j
public class MainTableCreator {

	private NTable<Link> table;

    private NTableColumn<Link,String>    columnGroup;
    private NTableColumn<Link,IconTitle> columnTitle;
    private NTableColumn<Link,NDate>     columnLastUsedDt;
    private NTableColumn<Link,Integer>   columnExecCount;

    @Autowired
	private MainController mainController;

    @Autowired
    private LinkExecutor linkExecutor;

    @Autowired
	private LinkMatcher matcher;

	// 다른 프로세스에서 keypress 이벤트를 일으키는 것을 방지하기 위한 장치
	private boolean onBlock = false;

	public NTable<Link> init( TableView<Link> tableView ) {

		table = new NTable<>( tableView )
			.setSelectionMode( SelectionMode.SINGLE )
			.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY )
			;

		assignColumns();

        columnGroup.bindValue( row -> row.getGroup() );
        columnTitle.bindValue( row -> row.getIconTitle() );
        columnLastUsedDt.bindValue( row -> row.getLastExecDate() );
        columnExecCount.bindValue( row -> row.getExecCount() );

        columnTitle.bindShape( drawTitleCell() );

        columnLastUsedDt.bindShape( ( cell, item, empty ) -> {
			cell.setText( empty ? null : Strings.nvl(item) );
        }).setAlignment( Pos.CENTER );

		columnExecCount.bindShape( ( cell, item, empty ) -> {
			cell.setText( empty ? null : Strings.nvl(item) );
		}).setAlignment( Pos.CENTER_RIGHT );

		columnTitle.setComparator( ( iconTitlePrev, iconTitleNext ) -> iconTitlePrev.getTitle().compareToIgnoreCase( iconTitleNext.getTitle() ) );

		setMouseEvent();
		setKeyEvent();
		setFilter();

		drawDetailView();

		return table;

	}

	private CellFormatter<Link, IconTitle> drawTitleCell() {
		return ( cell, item, empty ) -> {

			if( empty ) {
				cell.setGraphic( null );
				return;
			}

			HBox hbox = new HBox();

			hbox.setAlignment(Pos.CENTER_LEFT);

			ImageView imageIcon = new ImageView(item.getIcon());
			Label labelTitle = new Label(item.getTitle());

			HBox.setHgrow(imageIcon, Priority.ALWAYS);
			HBox.setHgrow(labelTitle, Priority.ALWAYS);
			HBox.setMargin(imageIcon, new Insets(0, 5, 0, 0));

			hbox.getChildren().addAll(imageIcon, labelTitle);

			cell.setGraphic( hbox );

			cell.setOnDragOver( event -> {
				Dragboard db = event.getDragboard();
				if ( db.hasFiles() ) {
					event.acceptTransferModes( TransferMode.LINK );
				}
			});

			cell.setOnDragDropped( event -> {

				Dragboard db = event.getDragboard();

				if ( db.hasFiles() ) {
					for( File fileDragged : db.getFiles() ) {
						Platform.runLater( () -> {
							Link link = cell.getTableRow().getItem();
							linkExecutor.execute( link, fileDragged );
						});
					}
				}

				event.setDropCompleted( true );
				event.consume();

			});

		};
	}

	private void drawDetailView() {
		table.focusedProperty().addListener( ( observable, oldValue, newValue ) -> {
            if( newValue == true && table.getSelectionModel().getFocusedIndex() <= 0 ) { // focus gained
                table.getSelectionModel().selectFirst();
            }
        });
		table.getSelectionModel().getSelectedItems().addListener( (ListChangeListener<Link>) change -> {
			if( change.getList().isEmpty() ) return;
			mainController.setDetailView( change.getList().get( 0 ) );
        });
	}

	private void setFilter() {

		// filter 트리거 설정
		mainController.inputKeyword.textProperty().addListener( table.getChangeListener() );
		mainController.inputGroup.textProperty().addListener( table.getChangeListener() );

		// 데이터 필터 설정
		table.setFilter( new TableViewDataFilter<>() {

			private List patternGroup;
			private List patternKeyword;

			@Override
			public void before( ObservableValue observable, Object oldValue, Object newValue ) {
				patternGroup = matcher.toPostfix( mainController.inputGroup.getText() );
				patternKeyword = matcher.toPostfix( mainController.inputKeyword.getText() );
			}

			@Override
			public Predicate<Link> test( ObservableValue observable, Object oldVal, Object newVal ) {
				return link -> {
					if ( !matcher.isGroupMatched( patternGroup, link ) ) return false;
					if ( !matcher.isKeywordMatched( patternKeyword, link ) ) return false;
					return true;
				};
			}

			@Override
			public void after( ObservableValue observable, Object oldValue, Object newValue ) {
				mainController.clearDetailView();
				mainController.printSearchResult();
			}

		});

	}

	private void setKeyEvent() {
		// Enter는 KEY_RELEASED 이벤트에서 action 이벤트로 계속 전파되 stop propagation 안됨
		table.addEventHandler( KeyEvent.KEY_PRESSED, event -> {

			KeyCode keyCode = event.getCode();

			if( keyCode == UNDEFINED || onBlock ) {
				event.consume();
				return;
			}

			log.trace( ">> table keypress event : {}, source : {}, target : {}", event, event.getSource(), event.getTarget() );

			switch ( keyCode ) {
				case ENTER :
					event.consume();
					mainController.printCommand( "" );
					onBlock = true;
					linkExecutor.execute( table.getFocusedItem() );

					FxThread.start( () -> {
						FxThread.sleep( KEYPRESS_BLOCK_WAIT_MILISEC );
						onBlock = false;
					});
					return;
				case DELETE :
					event.consume();
					mainController.printCommand( "" );
					mainController.deleteLink( null );
					return;

			}

			if( keyCode == KeyCode.DELETE ) {

				event.consume();
				mainController.printCommand( "" );
				mainController.deleteLink( null );

			} else if( keyCode == KeyCode.ESCAPE ) {

				Control prevControl = mainController.prevControlSearch;
				if( prevControl == null ) {
					prevControl = mainController.inputKeyword;
				}

				event.consume();
				prevControl.requestFocus();

				mainController.prevControlDetail = null;

			} else if( keyCode == KeyCode.TAB ) {

				if( ! event.isShiftDown() ) {

					Control prevControl = mainController.prevControlDetail;
					if( prevControl == null ) {
						prevControl = mainController.descGroupName;
					}

					event.consume();
					prevControl.requestFocus();

				}

			}

		});
	}

	private void setMouseEvent() {
		table.addEventHandler( MouseEvent.MOUSE_CLICKED, event -> {
			if( event.getClickCount() > 1 ) {
				linkExecutor.execute( table.getFocusedItem() );
			}
		});
	}

	private void assignColumns() {

        columnGroup      = table.getColumn( "colGroup"      );
		columnTitle      = table.getColumn( "colTitle"      );
		columnLastUsedDt = table.getColumn( "colLastUsedDt" );
		columnExecCount  = table.getColumn( "colExecCount"  );

        columnExecCount.setStyle( "-fx-padding: 0 5 0 0" );

	}

}
