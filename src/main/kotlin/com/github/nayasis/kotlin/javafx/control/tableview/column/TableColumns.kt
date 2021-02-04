package com.github.nayasis.kotlin.javafx.control.tableview.column

import javafx.scene.control.TableColumn
import javafx.util.Callback
import tornadofx.*
import kotlin.reflect.KProperty1

inline fun <S,T> TableColumn<S,T>.bindVal(prop: KProperty1<S,T?>, noinline option: TableColumn<S,T>.() -> Unit = {}): TableColumn<S,T> {
    this.cellValueFactory = Callback { observable(it.value, prop) }
    return this.apply(option)
}

fun <S,T:Any> TableColumn<S,T>.findBy(fxId: String): TableColumn<S,T>? {
    if( id == fxId ) return this
    for( col in columns )
        return (col.findBy(fxId) ?: continue) as TableColumn<S,T>
    return null
}

fun <S,T:Any> TableColumn<S,T>.children(recursive: Boolean = false): List<TableColumn<S,T>> {
    return ArrayList<TableColumn<S,T>>().apply {
        addAll( this@children.columns as Collection<TableColumn<S,T>> )
        if( recursive )
            forEach{ addAll(it.children(recursive) as List<TableColumn<S,T>>) }
    }
}