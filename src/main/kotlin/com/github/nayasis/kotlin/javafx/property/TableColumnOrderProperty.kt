package com.github.nayasis.kotlin.javafx.property

import com.github.nayasis.kotlin.basica.nvl
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import java.io.Serializable

data class TableColumnOrderProperty(
    var columns: ArrayList<String> = ArrayList()
): Serializable {

    constructor(tableview: TableView<*>): this() {
        read(tableview)
    }

    fun read(tableview: TableView<*>) {
        for ((i, column) in tableview.sortOrder.withIndex()) {
            this.columns.add(if (column.id.isNullOrEmpty()) i.toString() else column.id)
        }
    }

    fun apply(tableView: TableView<Any>) {

        val sorted = arrayListOf<TableColumn<Any,*>>()
        val source = linkedMapOf<String,TableColumn<Any,*>>()
            tableView.columns.withIndex().forEach {
                val key = nvl(it.value.id, it.index.toString())
                source[key] = it.value
            }
        for( id in columns ) {
            if( id in source )
                sorted.add( source.remove(id)!! )
        }
        sorted.addAll( source.values )

        tableView.columns.apply {
            clear()
            addAll(sorted)
        }
        tableView.sortOrder.apply {
            clear()
            addAll(sorted)
        }

    }

}