package com.github.nayasis.kotlin.javafx.property

import javafx.scene.control.TableColumn
import java.io.Serializable

class TableColumnProperty(
    var fxid: String? = null,
    var width: Double? = null,
    var show: Boolean? = null,
    var sortType: TableColumn.SortType? = null,
    var children: List<TableColumnProperty>? = null
): Serializable {

    constructor(column: TableColumn<Any,*>) : this() {
        read(column)
    }

    fun read(column: TableColumn<Any,*> ) {
        fxid = column.id
        width = column.width
        show = column.isVisible
        sortType = column.sortType
        children = column.columns.map { TableColumnProperty(it) }
    }

    fun apply(column: TableColumn<Any,*>?) {

        if( column == null || column.id != fxid ) return

        fxid?.let{ column.id = it }
        width?.let{ column.prefWidth = it }
        show?.let{ column.isVisible = it }
        sortType?.let{ column.sortType = it }

        if( ! column.columns.isEmpty() && ! children.isNullOrEmpty() ) {
            val map = children?.associate { it.fxid!! to it } ?: emptyMap()
            column.columns.forEach { map[it.id]?.apply(it) }
        }

    }

}