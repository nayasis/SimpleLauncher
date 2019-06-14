package com.nayasis.simplelauncher.controller;

import com.nayasis.simplelauncher.Main;
import com.nayasis.simplelauncher.common.CONSTANT;
import com.nayasis.simplelauncher.service.LinkExecutor;
import com.nayasis.simplelauncher.service.MainTableCreator;
import com.nayasis.simplelauncher.vo.Link;
import io.nayasis.common.basica.base.Strings;
import io.nayasis.common.basica.model.Messages;
import io.nayasis.common.basicafx.desktop.Desktop;
import io.nayasis.common.basicafx.javafx.control.table.NTable;
import io.nayasis.common.basicafx.javafx.dialog.Dialog;
import io.nayasis.common.basicafx.javafx.etc.FxThread;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.nayasis.simplelauncher.common.CONSTANT.KEYPRESS_BLOCK_WAIT_MILISEC;
import static com.nayasis.simplelauncher.common.CONSTANT.STAGE.HELP;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyCode.TAB;
import static javafx.scene.input.KeyCode.UNDEFINED;

@Component
@Slf4j
public class MainController implements Initializable {

	@FXML public AnchorPane       root;
	@FXML public VBox             vboxTop;

	@FXML public MenuBar          menubarTop;
    @FXML public CheckMenuItem    menuitemViewDesc;
    @FXML public CheckMenuItem    menuitemViewMenuBar;
    @FXML public CheckMenuItem    menuitemAlwaysOnTop;
    @FXML public MenuItem         menuItemHelp;

    @FXML public TextField        inputKeyword;
    @FXML public TextField        inputGroup;

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
    private ConfigController configController;

    @Autowired
    private LinkExecutor executor;

    // 다른 프로세스에서 keypress 이벤트를 일으키는 것을 방지하기 위한 장치
    private Set<TextField> onBlock = new ConcurrentHashMap<TextField,Void>().newKeySet();

    // 테이블로 focus를 이동시키기 이전의 Control
	public Control prevControlSearch = null;
	public Control prevControlDetail = null;

    @Override
	public void initialize( URL url, ResourceBundle resourceBundle ) {

    	log.debug( ">> start initialize" );

    	tableMain = tableController.init( tableMainRaw );
    	Main.$.notifyPreloader( 20., "preloader.ui.made" );

		dataController.readData();
		Main.$.notifyPreloader( 80., "preloader.link-data.loaded" );

		printSearchResult();

		setPropertyEvent();
		setKeyPressEvent();

    }

	private void setKeyPressEvent() {

		root.setOnKeyPressed( this::onKeyPressed );

		tabPressed( descDescription );
		tabPressed( descCmdNext );
		tabPressed( descCmdPrev );

        keyPressedOnSearchInput( inputKeyword );
        keyPressedOnSearchInput( inputGroup );

        escPressedOnDescription( descGroupName        );
        escPressedOnDescription( descShowConsole      );
        escPressedOnDescription( descTitle            );
        escPressedOnDescription( descDescription      );
        escPressedOnDescription( descExecPath         );
        escPressedOnDescription( descExecOption       );
        escPressedOnDescription( descExecOptionPrefix );
        escPressedOnDescription( descCmdNext          );
        escPressedOnDescription( descCmdPrev          );

	}

	public void printStatus( Object value, Object... param ) {
		labelStatus.setText( Messages.get( value, param ) );
	}

	public void printSearchResult() {
		printStatus( "msg.info.005", tableMain.getDataOnView().size(), tableMain.getData().size() );
	}

	public void printCommand( String command ) {
    	labelCmd.setText( Strings.nvl(command) );
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

		Platform.runLater( () -> descGroupName.requestFocus() );

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
			clearDetailView();
    		tableMain.getSelectionModel().select( focusedIndex );
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
		descIcon.imageProperty().addListener( changeListener );
        descExecPath.textProperty().addListener( changeListener );
        descExecOption.textProperty().addListener( changeListener );
        descExecOptionPrefix.textProperty().addListener( changeListener );
        descCmdNext.textProperty().addListener( changeListener );
        descCmdPrev.textProperty().addListener( changeListener );

		// 보여주기 체크메뉴 기능
		menuitemViewDesc.selectedProperty().addListener( ( observable, oldValue, newValue ) -> {
			showDescription( newValue == Boolean.TRUE );
		});
		menuitemViewMenuBar.selectedProperty().addListener( ( observable, oldValue, newValue ) -> {
			showMenuBar( newValue == Boolean.TRUE );
		});
		menuitemAlwaysOnTop.selectedProperty().addListener( ( observable, oldValue, newValue ) -> {
			alwaysOnTop( newValue == Boolean.TRUE );
		});

		menuItemHelp.setAccelerator( new KeyCodeCombination(KeyCode.F1) );

		labelCmd.setOnMouseClicked( event -> {
			if( event.getClickCount() > 1 ) {
				Desktop.copyToClipboard( labelCmd.getText() );
			}
		});

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
		if( iconFile != null && linkDetail.setIcon( iconFile ) ) {
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

		if( link == null )
			link = new Link();

		try {

            // ID가 같을 경우, 다시 그리지 않는다.
            if( linkDetail.getId() == null ) {
                if( link.getId() == null ) return;
            } else {
                if( link.getId() != null && linkDetail.getId() == link.getId() ) return;
            }

            linkDetail = link;

            bindLinkToView();

        } finally {

            buttonDelete.setDisable( false );
            buttonCopy.setDisable( false );
            buttonSave.setDisable( true );
            printCommand( "" );

		}

    }

	public void clearDetailView() {

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

	public void showDescription( boolean show ) {
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
		ObservableList<Node> children = vboxTop.getChildren();
		if( show ) {
			if( ! children.contains( menubarTop ) )
				children.add( 0, menubarTop );
		} else {
			children.remove( menubarTop );
		}
	}

	public void alwaysOnTop( boolean yes ) {
		CONSTANT.STAGE.MAIN.setAlwaysOnTop( yes );
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
    	HELP.showLater();
	}

    private void onKeyPressed( KeyEvent event ) {

		KeyCode     keyCode = event.getCode();
		EventTarget target  = event.getTarget();

		log.trace( ">> Event {}", event );

		if( keyCode == UNDEFINED ) {
			event.consume();
			return;
		}

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
			return;
		}

		if( event.isControlDown() ) {

			if( ! event.isShiftDown() && isDescriptionShown() ) {
				switch ( keyCode ) {
					case S :
						saveLink(); break;
					case D :
						deleteLink( null ); break;
					case C :
						copyLink( null ); break;
					case N :
						createNewLink( null ); break;
					case I :
						changeIcon( new ActionEvent() ); break;
					case O :
						openFolder( null ); break;
					case F :
						copyFolder( null ); break;
					default :
						return;
				}
				event.consume();
			}

		}

	}

	private boolean isDescriptionShown() {
		return menuitemViewDesc.isSelected();
	}

	private void tabPressed( TextArea textArea ) {

		EventHandler<KeyEvent> eventHandler = event -> {

            KeyCode  keyCode = event.getCode();

            if( keyCode == UNDEFINED ) {
                event.consume();
                return;
            }

            log.trace( ">> Event {}", event );

			if ( event.getCode() != TAB || event.isShiftDown() || event.isControlDown() ) return;

			event.consume();

			KeyEvent newEvent = new KeyEvent(
				event.getSource(),
				event.getTarget(),
				event.getEventType(),
				event.getCharacter(),
				event.getText(),
				event.getCode(),
				event.isShiftDown(),
				true,
				event.isAltDown(),
				event.isMetaDown()
			);

			Node node = (Node) event.getSource();
			node.fireEvent( newEvent );

		};

		textArea.addEventFilter( KeyEvent.KEY_PRESSED, eventHandler );

	}

    private void keyPressedOnSearchInput( TextField textField ) {

        EventHandler<KeyEvent> eventHandler = event -> {

            KeyCode keyCode = event.getCode();

            if( keyCode == UNDEFINED || onBlock.contains(textField) ) {
                event.consume();
                return;
            }

            log.trace( ">> Event {}", event );

            switch ( keyCode ) {

                case ESCAPE :
                    event.consume();
                    tableMain.focus();
					prevControlSearch = textField;
                    return;

                case ENTER :

                    try {
                        if( tableMain.getVisibleRowSize() == 1 ) {
                            event.consume();
                            ObservableList<Link> links = tableMain.getDataOnView();
                            if( links.size() == 1 ) {
                                Link link = links.get( 0 );

                                onBlock.add( textField );

                                setDetailView( link );
                                executor.execute( link );

                                FxThread.start( () -> {
                                    FxThread.sleep( KEYPRESS_BLOCK_WAIT_MILISEC );
                                    onBlock.remove( textField );

                                });

                            }
                            return;
                        }
                    } catch ( Throwable e ) {
                        Dialog.error( e );
                    }
                    return;
            }

        };

        textField.addEventFilter( KeyEvent.KEY_PRESSED, eventHandler );

        // prevent key typed event by other process (like Rufus)
        textField.addEventHandler( KeyEvent.KEY_TYPED, event -> {
            if( onBlock.contains( textField ) ) {
                event.consume();
                return;
            }
        });

    }

    private void escPressedOnDescription( Control control ) {
        control.addEventHandler( KeyEvent.KEY_PRESSED, event -> {
            if( event.getCode() == ESCAPE ) {
                event.consume();
                tableMain.focus();
				prevControlDetail = control;
            }
        });
    }

}