package com.github.nayasis.kotlin.javafx.control.tableview

import com.github.nayasis.kotlin.javafx.control.tableview.column.children
import com.github.nayasis.kotlin.javafx.control.tableview.column.findBy
import com.github.nayasis.kotlin.basica.exception.NotFound
import com.github.nayasis.kotlin.basica.core.nvl
import com.sun.javafx.scene.control.skin.TableViewSkin
import com.sun.javafx.scene.control.skin.VirtualFlow
import javafx.collections.transformation.FilteredList
import javafx.collections.transformation.SortedList
import javafx.scene.control.TableColumn
import javafx.scene.control.TablePosition
import javafx.scene.control.TableView
import java.lang.Integer.min
import kotlin.collections.ArrayList
import kotlin.math.max

fun <S,T:Any> TableView<S>.findColumnBy(fxId: String): TableColumn<S,T> {
    for( col in columns )
        return (col.findBy(fxId) ?: continue) as TableColumn<S,T>
    throw NotFound("fxId:$fxId")
}

fun <S> TableView<S>.allColumns(): List<TableColumn<S,*>> {
    return ArrayList<TableColumn<S,*>>().apply {
        columns.forEach{
            this.add(it as TableColumn<S,*> )
            this.addAll( it.children(true) )
        }
    }
}

fun <S> TableView<S>.fillFxId(): TableView<S> {
    this.allColumns().withIndex().forEach {
        it.value.id = nvl( it.value.id, it.index.toString() )
    }
    return this
}

fun <S> TableView<S>.focused(): Position {
    return (focusModel.focusedCellProperty().get() as TablePosition<S,*>).let {
        return Position(it.row, it.column)
    }
}
data class Position(val row: Int, val col: Int )

fun <S> TableView<S>.select( row: Int, col: Int = -1, scroll: Boolean = true ) {
    selectionModel.clearSelection()
    if( col < 0 ) {
        selectionModel.select( row )
    } else {
        val colIndex = min( max(col, 0), visibleLeafColumns.size - 1 )
        val column = visibleLeafColumns[colIndex]
        selectionModel.select( row, column )
    }
    if(scroll) scroll(row)
}

fun <S> TableView<S>.selectBy( row: S? ): Int {
    return itemIndex(row).also { select(it,-1) }
}

private fun <S> TableView<S>.itemIndex(row: S?): Int {
    return when (row) {
        null -> -1
        is Int -> row
        else -> items.indexOf(row) ?: -1
    }
}

fun <S> TableView<S>.focus( row: Int, col: Int = -1 ) {
    select(row, col, false)
    requestFocus()
    if( col < 0 ) {
        focusModel.focus(row)
    } else {
        val colIndex = min( max(col, 0), visibleLeafColumns.size - 1 )
        val column = visibleLeafColumns[colIndex]
        focusModel.focus(row, column)
    }
    scroll(row)
}

fun <S> TableView<S>.focusBy( row: S? ): Int {
    return itemIndex(row).also { focus(it,-1) }
}

fun <S> TableView<S>.scroll( row: Int, middle: Boolean = true ) {
    val index = if( middle ) {
        max( row - (visibleRows() / 2), 0)
    } else {
        row
    }
    this.scrollTo( index )
}

fun <S> TableView<S>.scrollBy( row: S?, middle: Boolean = true ): Int {
    return itemIndex(row).also { scroll(it,middle) }
}


fun <S> TableView<S>.visibleRows(): Int {
    return virtualFlow()?.let{
        val first = it.firstVisibleCell.index
        val last  = it.lastVisibleCell.index
        last + first + 1
    } ?: 0
}

fun <S> TableView<S>.virtualFlow(): VirtualFlow<*>? {
    return (skin as TableViewSkin<S>)?.children?.firstOrNull { it is VirtualFlow<*> } as VirtualFlow<*>
}

fun <S> TableView<S>.setItems( list: FilteredList<S> ) {
    val sortedList = SortedList(list)
    sortedList.comparatorProperty().bind( this.comparatorProperty() )
    this.items = sortedList
}