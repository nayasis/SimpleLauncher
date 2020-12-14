package com.github.nayasis.kotlin.javafx.control.tableview

import com.github.nayasis.kotlin.javafx.control.tableview.column.findBy
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView


fun <S> TableView<S>.findColumnBy(fxId: String ): TableColumn<S, *>? {
    for( col in columns )
        return col.findBy(fxId) ?: continue
    return null
}