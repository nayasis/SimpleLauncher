package com.nayasis.simplelauncher.service;

import com.nayasis.simplelauncher.controller.MainController;
import com.nayasis.simplelauncher.vo.IconTitle;
import com.nayasis.simplelauncher.vo.Link;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.StringStack;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

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
		mainController.chkRegexSearch.selectedProperty().addListener( table.getChangeListener() );

		// 데이터 필터 설정
		table.setFilter( (observable, oldVal, newVal) -> {

			List patternGroup   = toPostfix( mainController.inputGroup.getText() );
			List patternKeyword = toPostfix( mainController.inputKeyword.getText() );

			return link -> {
				if( ! isGroupMatched( patternGroup,     link ) ) return false;
				if( ! isKeywordMatched( patternKeyword, link ) ) return false;
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

	private boolean isKeywordMatched( List postfix, Link link ) {
		return isMatched( postfix, link, (lnk, word) -> lnk.isKeywordMatched(word) );
	}

	private boolean isGroupMatched( List postfix, Link link ) {
		return isMatched( postfix, link, (lnk, word) -> lnk.isGroupMatched(word) );
	}

	private boolean isMatched( List postfix, Link link, Matcher matcher ) {

		if( postfix.isEmpty() ) return true;

		Stack<Boolean> stack = new Stack();

		for( Object exp : postfix ) {

			if( exp instanceof Operator ) {

				Boolean v1 = stack.pop();
				Boolean v2 = stack.pop();

				switch( (Operator) exp ) {
					case AND :
						stack.push( v1 && v2 );
						break;
					case OR :
						stack.push( v1 || v2 );
						break;
				}

			} else {
				stack.push( matcher.isMatched( link, (String) exp ) );
			}

		}

		return stack.pop();

	}

	private List toPostfix( String text ) {

		List postfix = new ArrayList();

		Stack operators = new Stack();

		for( Object exp : toExpression(text) ) {

			if( exp instanceof Operator ) {
				operators.push( exp );
			} else {
				postfix.add( exp );
				if( ! operators.isEmpty() ) {
					postfix.add( operators.pop() );
				}
			}

		}

		while( ! operators.isEmpty() ) {
			postfix.add( operators.pop() );
		}

		return postfix;

	}

	private List toExpression( String text ) {

		LinkedList expression = new LinkedList();

		for( String token : Strings.tokenize(text, " ,", true) ) {
		    if( token.equals( " " ) ) continue;
			if( token.equals(",") ) {
				if( ! expression.isEmpty() ) {
					if( ! (expression.peek() instanceof Operator) ) {
						expression.push( Operator.OR );
					}
				}
			} else {
				if( ! expression.isEmpty() ) {
					if( ! (expression.peek() instanceof Operator) ) {
						expression.push( Operator.AND );
					}
				}
				expression.push( token );
			}
		}

		if( expression.peek() instanceof Operator ) {
			expression.pop();
		}

        Collections.reverse( expression );

		return expression;

	}

	private enum Operator {
		AND, OR;
	}

	private interface Matcher {
		boolean isMatched( Link link, String keyword );
	}

}
