package com.github.nayasis.kotlin.javafx.property

import com.github.nayasis.kotlin.javafx.control.tableview.allColumns
import com.github.nayasis.kotlin.javafx.control.tableview.fillFxId
import com.github.nayasis.kotlin.javafx.control.tableview.focus
import com.github.nayasis.kotlin.javafx.control.tableview.focused
import javafx.scene.control.TableView
import java.io.Serializable

data class TableProperty(
    val columns: ArrayList<TableColumnProperty> = ArrayList(),
    var columnOrder: TableColumnOrderProperty? = null,
    var visible: Boolean = true,
    var focusedRow: Int = -1,
): Serializable{

    constructor(tableview: TableView<*>): this() {
        read(tableview)
    }

    fun read(tableview: TableView<*>) {
        tableview.fillFxId()
        visible = tableview.isVisible
        focusedRow = tableview.focused().row
        columns.addAll( tableview.allColumns().map{ TableColumnProperty(it) } )
        columnOrder = TableColumnOrderProperty(tableview)
    }

    fun apply(tableview: TableView<Any>) {

        tableview.fillFxId()

        columnOrder?.apply(tableview)

        val map = tableview.allColumns().associateBy { it.id }
        columns.forEach{ col -> col.fxid?.let { col.apply( map[it] ) } }

        tableview.isVisible = visible
        tableview.focus( focusedRow )

    }

}