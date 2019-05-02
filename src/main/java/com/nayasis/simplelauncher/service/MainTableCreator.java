package com.nayasis.simplelauncher.service;

import com.nayasis.simplelauncher.controller.MainController;
import com.nayasis.simplelauncher.vo.IconTitle;
import com.nayasis.simplelauncher.vo.Link;
import io.nayasis.common.basica.base.Strings;
import io.nayasis.common.basica.model.NDate;
import io.nayasis.common.basicafx.javafx.control.table.NTable;
import io.nayasis.common.basicafx.javafx.control.table.NTableColumn;
import io.nayasis.common.basicafx.javafx.control.table.byfunction.CellFormatter;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
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
//		table.focusedProperty().addListener( ( observable, oldValue, newValue ) -> {
//            if( newValue == true && table.getSelectionModel().getFocusedIndex() <= 0 ) { // focus gained
//                table.getSelectionModel().selectFirst();
//                mainController.drawDetailViewFromTable();
//            }
//        });
//		table.getSelectionModel().getSelectedItems().addListener( (ListChangeListener<Link>) change -> {
//            mainController.drawDetailViewFromTable();
//        });
	}

	private void setFilter() {

		// filter 트리거 설정
		mainController.inputKeyword.textProperty().addListener( table.getChangeListener() );
		mainController.inputGroup.textProperty().addListener( table.getChangeListener() );

		// 데이터 필터 설정
		table.setFilter( (observable, oldVal, newVal) -> {

			List patternGroup   = matcher.toPostfix( mainController.inputGroup.getText() );
			List patternKeyword = matcher.toPostfix( mainController.inputKeyword.getText() );

			return link -> {
				if( ! matcher.isGroupMatched( patternGroup,     link ) ) return false;
				if( ! matcher.isKeywordMatched( patternKeyword, link ) ) return false;
				return true;
			};
		});

	}

	private void setKeyEvent() {
		// Enter는 KEY_RELEASED 이벤트에서 action 이벤트로 계속 전파되 stop propagation 안됨
		table.addEventHandler( KeyEvent.KEY_PRESSED, event -> {

			log.trace( ">>> table keypress event : {}, source : {}, target : {}", event, event.getSource(), event.getTarget() );

			KeyCode keyCode = event.getCode();

			if( keyCode == KeyCode.ENTER ) {

				event.consume();

				mainController.labelCmd.setText( "" );
				linkExecutor.execute( table.getFocusedItem() );

			} else if( keyCode == KeyCode.DELETE ) {

				event.consume();

				mainController.labelCmd.setText( "" );
				mainController.deleteLink( null );

			} else if( keyCode == KeyCode.ESCAPE ) {
				mainController.inputKeyword.requestFocus();
			}

		});
	}

	private void setMouseEvent() {

		TableColumn column = table.getColumn( 0 ).getRaw();

//		NDate[] lastClickTime = new NDate[1];
//
//		table.getColumn( 0 ).getRaw().addEventHandler( MouseEvent.MOUSE_CLICKED, event -> {
//			log.debug( "click count : {}", event.getClickCount() );
//			log.debug( "\t {}", event );
//			mainController.drawDetailViewFromTable();
//			if( event.getClickCount() > 1 ) {
//				linkExecutor.execute( table.getFocusedItem() );
//			}
//		});

//		table.getRaw().setRowFactory( tv -> {
//			TableRow row = new TableRow<>();
//			row.setOnMouseClicked(e -> {
//				if (e.getClickCount() == 2 && (!row.isEmpty()) ) {
//					System.out.println( table.getRaw().getSelectionModel().getSelectedItem());
//				}
//			});
//			return row;
//		});

		table.addEventHandler( MouseEvent.MOUSE_CLICKED, event -> {
			log.debug( "click count : {}", event.getClickCount() );
			log.debug( "\t {}", event );
//			mainController.drawDetailViewFromTable();
			NDate now = new NDate();
			if( event.getClickCount() > 1 ) {
				linkExecutor.execute( table.getFocusedItem() );
			}
		});

//		table.getRaw().setOnMouseClicked( event -> {
//			log.debug( "click count : {}", event.getClickCount() );
//			log.debug( "\t {}", event );
//			if( event.getClickCount() > 1 ) {
//				linkExecutor.execute( table.getFocusedItem() );
//				mainController.drawDetailViewFromTable();
//			} else {
//				mainController.drawDetailViewFromTable();
//			}
//		});

	}

	private void assignColumns() {
        columnGroup      = table.getColumn( "colGroup"      );
		columnTitle      = table.getColumn( "colTitle"      );
		columnLastUsedDt = table.getColumn( "colLastUsedDt" );
		columnExecCount  = table.getColumn( "colExecCount"  );
	}

}
