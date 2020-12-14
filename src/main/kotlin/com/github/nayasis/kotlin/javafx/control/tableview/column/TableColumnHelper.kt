package com.github.nayasis.kotlin.javafx.control.tableview.column

import javafx.scene.control.TableColumn
import javafx.util.Callback
import tornadofx.*
import kotlin.reflect.KProperty1

fun <S> TableColumn<S,*>.bindVal(prop: KProperty1<S,*>): TableColumn<S,*> {
    this.cellValueFactory = Callback { observable(it.value, prop) }
    return this
}


fun <S> TableColumn<S,*>.findBy(fxId: String): TableColumn<S,*>? {
    if( id == fxId ) return this
    for( col in columns )
        return col.findBy(fxId) ?: continue
    return null
}

