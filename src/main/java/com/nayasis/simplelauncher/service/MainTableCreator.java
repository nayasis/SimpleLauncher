package com.nayasis.simplelauncher.service;

import com.nayasis.simplelauncher.controller.MainController;
import com.nayasis.simplelauncher.vo.IconTitle;
import com.nayasis.simplelauncher.vo.Link;
import io.nayasis.common.base.Strings;
import io.nayasis.common.model.NDate;
import io.nayasis.common.ui.javafx.control.table.NTable;
import io.nayasis.common.ui.javafx.control.table.NTableColumn;
import io.nayasis.common.ui.javafx.control.table.byfunction.CellFormatter;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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

			cell.addEventHandler( DragEvent.DRAG_OVER, event -> {
				Dragboard db = event.getDragboard();
				if ( db.hasFiles() ) {
					event.acceptTransferModes( TransferMode.COPY );
				} else {
					event.consume();
				}
			});

			cell.addEventHandler( DragEvent.DRAG_DROPPED, event -> {

				Dragboard db = event.getDragboard();

				boolean success = false;

				if ( db.hasFiles() ) {
					success = true;
					for( File fileDragged : db.getFiles() ) {
						Platform.runLater( () -> {
							Link link = (Link) cell.getTableRow().getItem();
							linkExecutor.execute( link, fileDragged );
						});
					}
				}

				event.setDropCompleted( success );
				event.consume();
			});

		};
	}

	private void drawDetailView() {
		table.focusedProperty().addListener( ( observable, oldValue, newValue ) -> {
            if( newValue == true && table.getSelectionModel().getFocusedIndex() <= 0 ) { // focus gained
                table.getSelectionModel().selectFirst();
                mainController.drawDetailViewFromTable();
            }
        });
		table.getSelectionModel().getSelectedItems().addListener( (ListChangeListener<Link>) change -> {
            mainController.drawDetailViewFromTable();
        });
	}

	private void setFilter() {

		// filter 트리거 설정
		mainController.inputKeyword.textProperty().addListener( table.getChangeListener() );
		mainController.inputGroup.textProperty().addListener( table.getChangeListener() );
		mainController.checkboxKeywordAnd.selectedProperty().addListener( table.getChangeListener() );
		mainController.checkboxGroupAnd.selectedProperty().addListener( table.getChangeListener() );

		// 데이터 필터 설정
		table.setFilter( (observable, oldVal, newVal) -> {

			String  keyword          = mainController.inputKeyword.getText();
			boolean keywordAndSearch = mainController.checkboxKeywordAnd.isSelected();
			String  group            = mainController.inputGroup.getText();
			boolean groupAndSearch   = mainController.checkboxGroupAnd.isSelected();

			Pattern patternGroup     = toPattern( group,   groupAndSearch   );
			Pattern patternKeyword   = toPattern( keyword, keywordAndSearch );

			return link -> {
				if (patternGroup == null && patternKeyword == null) return true;
				if (patternGroup != null && ! link.isGroupMatched(patternGroup)) return false;
				return link.isKeywordMatched( patternKeyword );
			};
		});

	}

	private void setKeyEvent() {
		// Enter는 KEY_RELEASED 이벤트에서 action 이벤트로 계속 전파되 stop propagation 안됨
		table.addEventHandler( KeyEvent.KEY_PRESSED, event -> {

			log.debug( ">>> table keypress event : {}, source : {}, target : {}", event, event.getSource(), event.getTarget() );

			KeyCode keyCode = event.getCode();

			if( keyCode == KeyCode.ENTER ) {

				event.consume();

				mainController.labelCmd.setText( "" );
				linkExecutor.execute( table.getFocusedItem() );

			} else if( keyCode == KeyCode.DELETE ) {

				log.debug( "Delete action" );

				event.consume();

				mainController.labelCmd.setText( "" );
				mainController.deleteLink( null );

			}

		});
	}

	private void setMouseEvent() {
		table.addEventHandler( MouseEvent.MOUSE_CLICKED, event -> {
			mainController.drawDetailViewFromTable();
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
	}

	private Pattern toPattern( String text, boolean isAnd ) {

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

}
