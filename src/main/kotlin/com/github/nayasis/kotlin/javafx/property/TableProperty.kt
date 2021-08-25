package com.github.nayasis.kotlin.javafx.property

import com.github.nayasis.kotlin.basica.core.extention.ifNotNull
import com.github.nayasis.kotlin.javafx.control.tableview.allColumns
import com.github.nayasis.kotlin.javafx.control.tableview.fillFxId
import com.github.nayasis.kotlin.javafx.control.tableview.focus
import com.github.nayasis.kotlin.javafx.control.tableview.focused
import javafx.application.Platform
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import java.io.Serializable

data class TableProperty(
    val columns: LinkedHashMap<String,TableColumnProperty> = LinkedHashMap(),
    var columnSortOrder: TableColumnSortOrderProperty? = null,
    var visible: Boolean? = null,
    var focusedRow: Int? = null,
): Serializable{

    constructor(tableview: TableView<*>): this() {
        read(tableview)
    }

    fun read(tableview: TableView<*>) {
        visible = tableview.isVisible
        focusedRow = tableview.focused.row
        columnSortOrder = TableColumnSortOrderProperty(tableview)
        tableview.columns.forEach {
            columns[it.id] = TableColumnProperty(it)
        }
    }

    fun bind(tableview: TableView<Any>) {

        tableview.fillFxId()

        visible?.let { tableview.isVisible = it }
        reorderColumns(tableview)
        columnSortOrder?.bind(tableview)
        focusedRow?.let {
            Platform.runLater {
                tableview.focus(it)
            }
        }

    }

    private fun reorderColumns(tableview: TableView<Any>) {

        val sorted = arrayListOf<TableColumn<Any, *>>()
        val remain = linkedMapOf<String, TableColumn<Any, *>>()
            tableview.columns.forEach { remain[it.id] = it }

        columns.forEach { fxid, property ->
            remain.remove(fxid).ifNotNull {
                sorted.add(it)
                property.bind(it)
            }
        }

        sorted.addAll(remain.values)

        tableview.columns.clear()
        tableview.columns.addAll(sorted)

    }

}