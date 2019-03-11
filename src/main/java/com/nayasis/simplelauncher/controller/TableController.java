package com.nayasis.simplelauncher.controller;

import com.nayasis.simplelauncher.vo.IconTitle;
import com.nayasis.simplelauncher.vo.Link;
import io.nayasis.common.base.Strings;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class TableController {

	private SimpleLauncher  ui;
	private TableView<Link> table;

    @FXML public TableColumn<Link, String>     tableColumnGroupName;
    @FXML public TableColumn<Link, IconTitle>  tableColumnTitle;
    @FXML public TableColumn<Link, String>     tableColumnLastUsedDt;
    @FXML public TableColumn<Link, Number>     tableColumnExecCount;

	public TableController( SimpleLauncher ui ) {
		this.ui    = ui;
		this.table = ui.tableMain;
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
    private void assignColumns() {

		for( TableColumn column : table.getColumns() ) {
			log.debug( column.toString() );
		}

		tableColumnGroupName  = (TableColumn<Link, String>)    this.table.getColumns().get( 0 );
		tableColumnTitle      = (TableColumn<Link, IconTitle>) this.table.getColumns().get( 1 );
		tableColumnLastUsedDt = (TableColumn<Link, String>)    this.table.getColumns().get( 2 );
		tableColumnExecCount  = (TableColumn<Link, Number>)    this.table.getColumns().get( 3 );

	}

	public void init() {

		assignColumns();

		tableColumnGroupName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getGroupName()));
		tableColumnLastUsedDt.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getLastUsedDt()));
		tableColumnLastUsedDt.setCellFactory(column -> {

			TableCell<Link, String> cell = new TableCell<Link, String>() {
				public void updateItem(String item, boolean empty) {
					super.updateItem(item, empty);
					setText(empty ? "" : Strings.nvl(item));
					setGraphic(null);
				}
			};
			cell.setAlignment(Pos.CENTER);
			return cell;

		});

		tableColumnExecCount.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getExecCount()));
		tableColumnExecCount.setCellFactory( column -> {

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

		tableColumnTitle.setCellValueFactory( new PropertyValueFactory<>( "iconTitle" ) );
		tableColumnTitle.setCellFactory( column -> {

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
							ui.getDataController().executeLink( link, fileDragged );
						});
					}
                }

                event.setDropCompleted( success );
                event.consume();
            });

			return cell;

		});

		tableColumnTitle.setComparator( ( iconTitlePrev, iconTitleNext ) -> iconTitlePrev.getTitle().compareToIgnoreCase( iconTitleNext.getTitle() ) );

		table.addEventHandler( MouseEvent.MOUSE_CLICKED, event -> {

			ui.renderDetailViewFromTable( table );
			if( event.getClickCount() > 1 ) {
				ui.getDataController().executeLink( table.getSelectionModel().getSelectedItem() );
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

	        		ui.labelCmd.setText( "" );
	        		ui.getDataController().executeLink();

	        	} else if( keyCode == KeyCode.DELETE ) {

	        		log.debug( "Delete action" );

	        		event.consume();

	        		ui.labelCmd.setText( "" );
	        		ui.deleteLink( null );

	        		flagTableKeypressEvent = 1;

	        	}

	        }
        });

		table.focusedProperty().addListener( ( observable, oldValue, newValue ) -> {
            if( newValue == true && table.getSelectionModel().getFocusedIndex() <= 0 ) { // focus gained
                table.getSelectionModel().selectFirst();
                ui.renderDetailViewFromTable( table );
            }
        } );


		table.getSelectionModel().setSelectionMode( SelectionMode.SINGLE );

		table.getSelectionModel().getSelectedItems().addListener( (ListChangeListener<Link>) change -> {
            ui.renderDetailViewFromTable( table );
        });

		table.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );

	}

}
