package com.github.nayasis.kotlin.javafx.property

import com.github.nayasis.kotlin.javafx.control.tableview.allColumns
import com.github.nayasis.kotlin.javafx.control.tableview.fillFxId
import javafx.scene.control.TableView
import java.io.Serializable

data class TableColumnSortOrderProperty(
    var sortOrder: ArrayList<String> = ArrayList()
): Serializable {

    constructor(tableview: TableView<*>): this() {
        read(tableview)
    }

    fun read(tableview: TableView<*>) {
        tableview.sortOrder.forEach { sortOrder.add(it.id) }
    }

    fun bind(tableView: TableView<Any>) {
        if(sortOrder.isNotEmpty()) {
            val columnById = tableView.allColumns.filter { !it.id.isNullOrEmpty() }.associateBy { it.id }
            tableView.sortOrder.also {
                it.clear()
                it.addAll(sortOrder.mapNotNull { columnById[it] })
            }
        }
    }

}