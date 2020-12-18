package com.github.nayasis.kotlin.javafx.control.tableview

import com.github.nayasis.kotlin.javafx.control.tableview.column.findBy
import com.github.nayasis.kotlin.javafx.exception.NotFound
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView

fun <S,T:Any> TableView<S>.findColumnBy(fxId: String): TableColumn<S,T> {
    for( col in columns )
        return (col.findBy(fxId) ?: continue) as TableColumn<S,T>
    throw NotFound("fxId:$fxId")
}