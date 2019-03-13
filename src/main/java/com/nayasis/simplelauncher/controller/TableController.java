package com.nayasis.simplelauncher.controller;

import com.nayasis.simplelauncher.vo.IconTitle;
import com.nayasis.simplelauncher.vo.Link;
import io.nayasis.common.base.Strings;
import io.nayasis.common.model.NDate;
import io.nayasis.common.ui.javafx.control.table.NfxTable;
import io.nayasis.common.ui.javafx.control.table.NfxTableCell;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
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
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;

@Component
@Slf4j
public class TableController {

	private NfxTable<Link> table;

    @FXML public TableColumn<Link,String>    columnGroup;
    @FXML public TableColumn<Link,IconTitle> columnTitle;
    @FXML public TableColumn<Link, NDate>    columnLastUsedDt;
    @FXML public TableColumn<Link,Number>    columnExecCount;

    @Autowired
	private MainController mainController;

	@SuppressWarnings( { "rawtypes", "unchecked" } )
    private void assignColumns() {
		columnGroup      = table.getColumn( "colGroup" );
		columnTitle      = table.getColumn( "colTitle" );
		columnLastUsedDt = table.getColumn( "colLastUsedDt" );
		columnExecCount  = table.getColumn( "colExecCount" );
	}

	public void init() {

		table = new NfxTable<>( mainController.tableMain );

		assignColumns();

		Link link = new Link();
		link.getGroup();

		table.bindValue( columnGroup, row -> new SimpleStringProperty(row.getGroup()) );
		table.bindValue( columnTitle, row -> new SimpleStringProperty(row.getTitle()) );
//		table.bindValue( columnLastUsedDt, row -> new SimpleObjectProperty(row.getLastExecDate()), Pos.CENTER );
		table.bindValue( columnExecCount, row -> new SimpleIntegerProperty(row.getExecCount()), Pos.CENTER_RIGHT );

		table.bindShape( columnTitle, new TableCell<Link,Link>() {
			public void updateItem(Link item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty ? "" : Strings.nvl(item));
				setGraphic(null);
			}
		}, Pos.CENTER );

		table.bindShape( columnLastUsedDt, new TableCell<Link,NDate>() {
			public void updateItem(NDate item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty ? "" : Strings.nvl(item));
				setGraphic(null);
			}
		}, Pos.CENTER );

		table.bindShape( columnLastUsedDt, new NfxTableCell() {

			@Override
			public TableCell bind( TableColumn column ) {
				return null;
			}
		});

		columnGroup.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getGroup()));
		columnLastUsedDt.setCellValueFactory(cellData -> new SimpleObjectProperty(cellData.getValue().getLastExecDate()));
		columnLastUsedDt.setCellFactory(column -> {
			TableCell<Link, LocalDateTime> cell = new TableCell<Link, LocalDateTime>() {
				public void updateItem(LocalDateTime item, boolean empty) {
					super.updateItem(item, empty);
					setText(empty ? "" : Strings.nvl(item));
					setGraphic(null);
				}
			};
			cell.setAlignment(Pos.CENTER);
			return cell;
		});

		columnExecCount.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getExecCount()));
		columnExecCount.setCellFactory( column -> {
			TableCell<Link, Number> cell = new TableCell<Link, Number>() {
				public void updateItem( Number item, boolean empty) {
					super.updateItem( item, empty );
					setText( empty ? "" : Strings.nvl(item) );
					setGraphic( null );

				}
			};
			cell.setAlignment( Pos.CENTER_RIGHT );
			return cell;
		});

		columnTitle.setCellValueFactory( new PropertyValueFactory<>( "iconTitle" ) );
		columnTitle.setCellFactory( column -> {

			TableCell<Link, IconTitle> cell = new TableCell<Link, IconTitle>() {

                protected void updateItem( IconTitle item, boolean empty ) {

                	super.updateItem( item, empty );

                	if( empty || item == null ) {

                		setText( null );
                		setGraphic( null );

                		return;

                	}

					HBox hbox = new HBox();

					hbox.setAlignment( Pos.CENTER_LEFT );

					ImageView imageIcon  = new ImageView( item.getIcon() );
					Label     labelTitle = new Label( item.getTitle() );

					HBox.setHgrow( imageIcon,  Priority.ALWAYS );
					HBox.setHgrow( labelTitle, Priority.ALWAYS );
					HBox.setMargin( imageIcon, new Insets( 0, 5, 0, 0 ) );

					hbox.getChildren().addAll( imageIcon, labelTitle );

					setGraphic( hbox );

				}

			};

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
							mainController.getDataController().executeLink( link, fileDragged );
						});
					}
                }

                event.setDropCompleted( success );
                event.consume();
            });

			return cell;

		});

		columnTitle.setComparator( ( iconTitlePrev, iconTitleNext ) -> iconTitlePrev.getTitle().compareToIgnoreCase( iconTitleNext.getTitle() ) );

		table.addEventHandler( MouseEvent.MOUSE_CLICKED, event -> {

			mainController.renderDetailViewFromTable( table );
			if( event.getClickCount() > 1 ) {
				mainController.getDataController().executeLink( table.getFocusedItem() );
			}

		});

		table.addEventHandler( KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {

			private int flagTableKeypressEvent = 0;

			public void handle( KeyEvent event ) {

				log.debug( ">>> table keypress event : {}, source : {}, target : {}", event, event.getSource(), event.getTarget() );

	        	KeyCode keyCode = event.getCode();

	        	if( keyCode == KeyCode.ENTER ) {

	        		// Dialog에서 Event Propagation을 막지 못해, 지저분한 방법으로 처리
	        		if( flagTableKeypressEvent != 0 ) {
	        			flagTableKeypressEvent = 0;
	        			return;
	        		}

	        		event.consume();

	        		mainController.labelCmd.setText( "" );
	        		mainController.getDataController().executeLink();

	        	} else if( keyCode == KeyCode.DELETE ) {

	        		log.debug( "Delete action" );

	        		event.consume();

	        		mainController.labelCmd.setText( "" );
	        		mainController.deleteLink( null );

	        		flagTableKeypressEvent = 1;

	        	}

	        }
        });

		table.focusedProperty().addListener( ( observable, oldValue, newValue ) -> {
            if( newValue == true && table.getSelectionModel().getFocusedIndex() <= 0 ) { // focus gained
                table.getSelectionModel().selectFirst();
                mainController.renderDetailViewFromTable( table );
            }
        });


		table.getSelectionModel().setSelectionMode( SelectionMode.SINGLE );

		table.getSelectionModel().getSelectedItems().addListener( (ListChangeListener<Link>) change -> {
            mainController.renderDetailViewFromTable( table );
        });

		table.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );

	}

}
