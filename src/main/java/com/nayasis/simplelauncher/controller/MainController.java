package com.nayasis.simplelauncher.controller;

import com.nayasis.simplelauncher.common.CONSTANT;
import com.nayasis.simplelauncher.service.LinkExecutor;
import com.nayasis.simplelauncher.service.MainTableCreator;
import com.nayasis.simplelauncher.vo.Link;
import io.nayasis.common.base.Strings;
import io.nayasis.common.model.Messages;
import io.nayasis.common.ui.javafx.control.table.NTable;
import io.nayasis.common.ui.javafx.dialog.Dialog;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

@Component
@Slf4j
public class MainController implements Initializable {

	@FXML public AnchorPane       root;
	@FXML public VBox             vboxTop;

	@FXML public MenuBar          menubarTop;
    @FXML public CheckMenuItem    menuitemViewDesc;
    @FXML public CheckMenuItem    menuitemViewMenuBar;
    @FXML public MenuItem         menuItemHelp;

    @FXML public TextField        inputKeyword;
    @FXML public TextField        inputGroup;

          public ListView<String> listKeywordHistory = new ListView<>();

    @FXML public Button           buttonNew;
    @FXML public Button           buttonCopy;
    @FXML public Button           buttonSave;
    @FXML public Button           buttonDelete;
    @FXML public Button           buttonChangeIcon;
    @FXML public Button           buttonOpenFolder;

    @FXML public GridPane         descGridPane;
    @FXML public TextField        descGroupName;
    @FXML public CheckBox         descShowConsole;
    @FXML public TextField        descTitle;
    @FXML public TextArea         descDescription;
    @FXML public ImageView        descIcon;
    @FXML public TextField        descExecPath;
    @FXML public TextField        descExecOption;
    @FXML public TextField        descExecOptionPrefix;
    @FXML public TextArea         descCmdNext;
    @FXML public TextArea         descCmdPrev;

    @FXML public Label            labelStatus;
    @FXML public Label            labelCmd;

    @FXML public TableView<Link>  tableMainRaw;
	      public NTable<Link>     tableMain;

    private Link linkDetail = new Link();

	@Autowired
    private DataController dataController;

	@Autowired
    private MainTableCreator tableController;

    @Autowired
    private LinkExecutor executor;

    @Autowired
    private ConfigController configController;

    @Override
	public void initialize( URL url, ResourceBundle resourceBundle ) {

    	log.debug( ">> start initialize" );

    	tableMain = tableController.init( tableMainRaw );
		log.debug( ">> initTable" );

		log.debug( ">> bindConfigUi" );

		dataController.readData();
		log.debug( ">> readData" );

		setPropertyEvent();
		setKeywordHistoryDropdownList();

    }

	private void setKeywordHistoryDropdownList() {

		inputKeyword.focusedProperty().addListener(( observable, focusedOut, focusedIn ) -> {
            if( focusedOut == true ) {
                String keyword = inputKeyword.getText();
                if( Strings.isEmpty(keyword) ) return;
                configController.getKeywordHistory().put( keyword, null );
            }
        });

		listKeywordHistory.setVisible( false );
		listKeywordHistory.focusedProperty().addListener(( observable, focusedOut, focusedIn ) -> {
            if( focusedIn == true ) {
                listKeywordHistory.getSelectionModel().selectFirst();
            } else if( focusedOut == true ) {
                listKeywordHistory.setVisible( false );
            }
            log.debug( "focusedOut:{}, focusedIn:{}", focusedOut, focusedIn );
        });

		listKeywordHistory.addEventHandler( KeyEvent.KEY_RELEASED, event -> {

            KeyCode keyCode = event.getCode();

            if( keyCode == KeyCode.ENTER ) {
                event.consume();
                String selectedValue = listKeywordHistory.getSelectionModel().getSelectedItem();
                inputKeyword.setText( Strings.nvl(selectedValue) );
                inputKeyword.requestFocus();

            } else if( keyCode == KeyCode.ESCAPE ) {
                event.consume();
                inputKeyword.requestFocus();
            }

        });

		inputKeyword.widthProperty().addListener(( observable, oldValue, newValue ) -> listKeywordHistory.setPrefWidth( newValue.doubleValue() ));

		root.getChildren().add( listKeywordHistory );

	}

	public void printStatus( Object value, Object... param ) {
		labelStatus.setText( Messages.get( value, param ) );
	}

	@FXML
	public void loadFromFile( ActionEvent event ) {

		int prevIndex = tableMain.getSelectionModel().getSelectedIndex();

		dataController.readData();

		tableMain.getSelectionModel().select( prevIndex );

		setDetailView( tableMain.getFocusedItem() );

		Dialog.alert( "msg.info.008" );

	}

	@FXML
	public void importData( ActionEvent event ) {
		dataController.importData();
	}

	@FXML
	public void clearData( ActionEvent event ) {
		dataController.clearData();
	}

	@FXML
	public void exportData( ActionEvent event ) {
		dataController.exportData();
	}

	@FXML
	public void createNewLink( ActionEvent event ) {

		Link link = new Link();

		link.setIcon( CONSTANT.ICON_NEW );

		setDetailView( link );

		buttonDelete.setDisable( true );
		buttonCopy.setDisable( true );

	}

	@FXML
	public void saveLink( ActionEvent event ) {

		Platform.runLater( () -> buttonSave.requestFocus() );

		saveLink();

	}

	private void saveLink() {

		bindViewToLink();

		if( linkDetail.getId() == null ) {
			dataController.add( linkDetail );
			bindLinkToView();
		} else {
			dataController.update( linkDetail );
		}

		buttonDelete.setDisable( false );
		buttonCopy.setDisable( false );
		buttonSave.setDisable( true );

	}

	@FXML
	public void deleteLink( ActionEvent event ) {

		Link link = tableMain.getFocusedItem();
		if( link == null ) return;

		String linkInfo = Strings.isEmpty(link.getGroup())
				? link.getTitle().get()
				: link.getGroup().get() + " : " + link.getTitle().get();

		if( ! Dialog.confirm( "msg.confirm.001", linkInfo ) ) return;

		int focusedIndex = tableMain.getFocusedIndex();

		dataController.delete( link );

    	Platform.runLater( () -> {
    		tableMain.getSelectionModel().select( focusedIndex );
			clearDetailView();
    	});

	}

	@FXML
	public void copyLink( ActionEvent event ) {

		if( linkDetail.getId() == null ) return;

		linkDetail = linkDetail.clone();

		linkDetail.clearId();

		bindLinkToView();

		buttonDelete.setDisable( true );
		buttonCopy.setDisable( false );

	}

	@FXML
	public void addFile( MouseEvent event ) {
		File file = Dialog.filePicker( "msg.info.001", "msg.info.006", "*.*" ).showOpenDialog( CONSTANT.STAGE.MAIN );
		addFile( file );
	}

	@FXML
	public void eventDragOverByFile( DragEvent event ) {
      Dragboard db = event.getDragboard();
      if ( db.hasFiles() ) {
          event.acceptTransferModes( TransferMode.COPY );
      } else {
          event.consume();
      }
	}

	private void setPropertyEvent() {

        ChangeListener changeListener = ( observableValue, oldValue, newValue ) -> {
			buttonSave.setDisable( false );
		};

        descGroupName.textProperty().addListener( changeListener );
        descShowConsole.selectedProperty().addListener( changeListener );
        descTitle.textProperty().addListener( changeListener );
        descDescription.textProperty().addListener( changeListener );
        descExecPath.textProperty().addListener( changeListener );
        descExecOption.textProperty().addListener( changeListener );
        descExecOptionPrefix.textProperty().addListener( changeListener );
        descCmdNext.textProperty().addListener( changeListener );
        descCmdPrev.textProperty().addListener( changeListener );
		descTitle.textProperty().addListener( changeListener );

		// 이미지 변경시 focus가 Image에 귀속되어, 이를 변경해 주지 않으면 다른 이벤트(버튼 fire 이벤트 등)가 발생하지 않는다.
		descIcon.imageProperty().addListener( changeListener );

		// 보여주기 체크메뉴 기능
		menuitemViewDesc.selectedProperty().addListener( ( observable, oldValue, newValue ) -> {
			showDescription( newValue == Boolean.TRUE );
		});
		menuitemViewMenuBar.selectedProperty().addListener( ( observable, oldValue, newValue ) -> {
			showMenuBar( newValue == Boolean.TRUE );
		});

		menuItemHelp.setAccelerator( new KeyCodeCombination(KeyCode.F1) );

	}

	private interface EventRunner {
		void run( File file );
	}

	private void eventDragDropped( DragEvent event, EventRunner runnable ) {

		Dragboard db = event.getDragboard();

		boolean success = false;

		if ( db.hasFiles() ) {

			success = true;

			if( db.getFiles().size() > 1 ) {

				Dialog.alert( "msg.warn.001" );

			} else {
				File file = db.getFiles().get( 0 );
				Platform.runLater(() -> runnable.run( file ));
			}

		}

		event.setDropCompleted( success );
		event.consume();

	}

	@FXML
	public void eventDragDroppedOnFileAdd( DragEvent event ) {
		eventDragDropped( event, file -> {
			addFile( file );
		});
	}

	@FXML
	public void eventDragDroppedOnChangeIcon( DragEvent event ) {
		eventDragDropped( event, file -> {
			changeIcon( file );
			buttonSave.requestFocus();
		});
	}

	@FXML
	public void eventDragDroppedOnPath( DragEvent event ) {

		eventDragDropped( event, file -> {

            // 에러를 발생시키면, 이벤트를 consume 하지 못하고 테이블을 focus ! 하기 때문에.. 먼저 이벤트 발생 객체를 우선 focus 시켰다.
            descExecPath.requestFocus();

            linkDetail.setPath( file );

            if( linkDetail.getIcon().equals( CONSTANT.ICON_NEW ) ) {
                linkDetail.setIcon( file );
            }

            bindLinkToView();

        });

	}

	@FXML
	public void changeIcon( ActionEvent event ) {
		File iconFile = getIconFile();
		changeIcon( iconFile );
	}

	private void changeIcon( File iconFile ) {
		if( linkDetail.setIcon( iconFile ) ) {
			bindLinkToView();
		}
	}

	private File getIconFile() {
		return Dialog.filePicker( "msg.info.002", "Icon File", "*.*" ).showOpenDialog( CONSTANT.STAGE.MAIN );
	}

	private void addFile( File file ) {

		if( file == null ) return;

		if( ! file.exists() ) {
			Dialog.alert( "msg.error.001", file );
			return;
		}

		setDetailView( new Link(file) );

		buttonDelete.setDisable( true );
		buttonCopy.setDisable( true );
		buttonSave.setDisable( false );

		Platform.runLater( () -> {
			CONSTANT.STAGE.MAIN.requestFocus();
			descGroupName.requestFocus();
		});

	}

	public void setDetailView( Link link ) {

		if( link == null ) {
			link = new Link();
		}

		linkDetail = link;

		bindLinkToView();

		buttonDelete.setDisable( false );
		buttonCopy.setDisable( false );
		buttonSave.setDisable( true );

	}

	public void clearDetailView() {

		log.debug( "ClearDetailView is Called !!!" );

		setDetailView( null );

		buttonDelete.setDisable( true );
		buttonCopy.setDisable( true );
		buttonSave.setDisable( true );

	}

	private void bindLinkToView() {

		descTitle.setText( linkDetail.getTitle().get() );
		descShowConsole.setSelected( linkDetail.getShowConsole() );
		descGroupName.setText( linkDetail.getGroup().get() );
		descDescription.setText( linkDetail.getDescription() );
		descExecPath.setText( linkDetail.getPath() );
		descExecOption.setText( linkDetail.getOption() );
		descExecOptionPrefix.setText( linkDetail.getOptionPrefix() );
		descCmdPrev.setText( linkDetail.getCommandPrev() );
		descCmdNext.setText( linkDetail.getCommandNext() );
		descIcon.setImage( linkDetail.getIcon() );

	}

	private void bindViewToLink() {

		linkDetail.setTitle( descTitle.getText() );
		linkDetail.setShowConsole( descShowConsole.isSelected() );
		linkDetail.setGroup( descGroupName.getText() );
		linkDetail.setDescription( descDescription.getText() );
		linkDetail.setPath( descExecPath.getText() );
		linkDetail.setOption( descExecOption.getText() );
		linkDetail.setOptionPrefix( descExecOptionPrefix.getText() );
		linkDetail.setCommandPrev( descCmdPrev.getText() );
		linkDetail.setCommandNext( descCmdNext.getText() );
		linkDetail.setIcon( descIcon.getImage() );

	}

	public void drawDetailViewFromTable() {

		labelCmd.setText( "" );

		Link data = tableMain.getFocusedItem();
		if( data != null ) {
			setDetailView( data );
		}

	}

	public void showDescription( boolean show ) {
		log.debug( ">> show desc : {}", show );
		HBox parent = (HBox) tableMainRaw.getParent();
		ObservableList<Node> children = parent.getChildren();

		if( show ) {
			if( ! children.contains( descGridPane ) )
				children.add( descGridPane );
		} else {
			children.remove( descGridPane );
		}

	}

	public void showMenuBar( boolean show ) {
    	log.debug( ">> show menubar: {}", show );
		ObservableList<Node> children = vboxTop.getChildren();
		if( show ) {
			if( ! children.contains( menubarTop ) )
				children.add( 0, menubarTop );
		} else {
			children.remove( menubarTop );
		}
	}

	@FXML
	public void openFolder( ActionEvent event ) {
		executor.openFolder( linkDetail );
	}

	@FXML
	public void copyFolder( ActionEvent event ) {
		executor.copyFolder( linkDetail );
	}

	@FXML
	public void showHelp( ActionEvent event ) {
    	CONSTANT.STAGE.HELP.showLater();
	}

    private void showKeywordHistory() {

    	listKeywordHistory.getItems().clear();

    	String currentKeyword = inputKeyword.getText();

    	for( String keyword : configController.getKeywordHistory().keySet() ) {
    		if( currentKeyword.equals( keyword ) ) continue;
    		listKeywordHistory.getItems().add( keyword );
    	}

    	int rowSize    = listKeywordHistory.getItems().size();
    	int maxRowSize = 8;
    	int ROW_HEIGHT = 22;

    	log.debug( "rowSize : {}", rowSize );

    	if( rowSize == 0 ) return;

    	listKeywordHistory.setPrefHeight( ROW_HEIGHT * Math.min( rowSize, maxRowSize ) );

    	double marginTop  = 5.;
    	double marginLeft = 4.;

    	double x      = inputKeyword.getLayoutX();
		double y      = inputKeyword.getLayoutY();
		double height = inputKeyword.getHeight();

		listKeywordHistory.setLayoutX( x + marginLeft );
		listKeywordHistory.setLayoutY( y + marginTop + height );
		listKeywordHistory.setVisible( true );


    	log.debug( "listKeywordHistory.getPrefHeight() : {}", listKeywordHistory.getPrefHeight() );

		listKeywordHistory.requestFocus();

    }


	/**
	 * UI 에서 입력하는 모든 단축키 Event를 설정한다.
	 *
	 * @param event
	 */
	@FXML
	public void globalKeyPressedEvent( KeyEvent event ) {

		Object  source  = event.getSource();
		KeyCode keyCode = event.getCode();
		String  nodeId  = ( (Node) source ).getId();

		log.debug( "Event id:{}, keyCode:{}, source:{}", nodeId, event.getCode(), source );

		// 한개의 키코드로 단축키가 동작하지 않는 오류 보정로직
		if( keyCode == KeyCode.F1 ) {
			showHelp( null );
			event.consume();
			return;
		}

		// 체크박스 메뉴 동작오류 보정로직
		// (최초로딩시, 메뉴바를 숨김이 아닌 제거로 처리시, 단축키가 동작하지 않아 강제로 이벤트 설정)
		if( event.isAltDown() ) {
			switch ( keyCode ) {
				case E :
					menuitemViewDesc.setSelected( ! menuitemViewDesc.isSelected() );
					event.consume();
					return;
				case V :
					menuitemViewMenuBar.setSelected( ! menuitemViewMenuBar.isSelected() );
					event.consume();
					return;
			}
		}

		if( source == inputKeyword ) {

			switch ( keyCode ) {
				case DOWN :
					event.consume();
					showKeywordHistory();
					return;
				case ESCAPE :
					event.consume();
					tableMain.focus();
					return;
			}

		}

		if( event.isControlDown() ) {

			if( event.isShiftDown() ) {

				// bypass to menu

			} else {

				if( isDescriptionShown() ) {

					switch ( keyCode ) {
						case S :
							saveLink(); break;
						case D :
							deleteLink( null ); break;
						case C :
							copyLink( null );
						case N :
							createNewLink( null );
						case I :
							changeIcon( new ActionEvent() );
						case O :
							openFolder( null );
						case F :
							copyFolder( null );
						default :
							return;
					}

					event.consume();

				}

			}

		} else if( event.getCode() == KeyCode.TAB ) {

			if( ! event.isShiftDown() ) {

				if( source == descDescription ) {
					event.consume();
					descExecPath.requestFocus();
				} else if( source == descCmdPrev ) {
					event.consume();
					descCmdNext.requestFocus();
				} else if( source == descCmdNext ) {
					event.consume();
					inputKeyword.requestFocus();
				}

			} else if( event.isShiftDown() ) {

				// KeywordHistory Dropdown List : Shift + Tab
				if( source == root ) {
					event.consume();
					inputKeyword.requestFocus();
				}

			}

		}

	}

	private boolean isDescriptionShown() {
		return menuitemViewDesc.isSelected();
	}

}