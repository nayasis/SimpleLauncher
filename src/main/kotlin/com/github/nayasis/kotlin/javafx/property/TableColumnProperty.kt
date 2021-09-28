package com.github.nayasis.kotlin.javafx.property

import com.github.nayasis.kotlin.basica.core.extention.ifNotNull
import javafx.scene.control.TableColumn
import java.io.Serializable

data class TableColumnProperty(
    var fxid: String? = null,
    var width: Double? = null,
    var show: Boolean? = null,
    var sortType: TableColumn.SortType? = null,
    var children: LinkedHashMap<String,TableColumnProperty>? = null
): Serializable {

    constructor(column: TableColumn<*,*>) : this() {
        read(column)
    }

    fun read(column: TableColumn<*,*> ) {

        fxid = column.id
        width = column.width
        show = column.isVisible
        sortType = column.sortType

        if(!column.columns.isNullOrEmpty()) {
            children = LinkedHashMap()
            column.columns.forEach {
                children!![it.id] = TableColumnProperty(it)
            }
        }
    }

    fun bind(column: TableColumn<Any,*>) {

        width?.let{ column.prefWidth = it }
        show?.let{ column.isVisible = it }
        sortType?.let{ column.sortType = it }

        if( children != null ) {

            val sorted = arrayListOf<TableColumn<Any,*>>()
            val remain = linkedMapOf<String,TableColumn<Any,*>>()
                column.columns.forEach { remain[it.id] = it }

            children!!.forEach { fxid, property ->
                remain.remove(fxid).ifNotNull {
                    sorted.add(it)
                    property.bind(it)
                }
            }

            sorted.addAll(remain.values)

            column.columns.clear()
            column.columns.addAll(sorted)

        }

    }

}