package com.github.nayasis.kotlin.javafx.control.tableview.column

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.geometry.Pos
import javafx.scene.control.TableColumn
import javafx.util.Callback
import tornadofx.*
import kotlin.reflect.KProperty1

inline fun <S,T> TableColumn<S,T>.cellValue(prop: KProperty1<S,T?>, noinline option: TableColumn<S,T>.() -> Unit = {}): TableColumn<S,T> {
    this.cellValueFactory = Callback { observable(it.value, prop) }
    return this.apply(option)
}

fun <S,T> TableColumn<S,T>.cellValue(callback: Callback<TableColumn.CellDataFeatures<S, T>, ObservableValue<T>>): TableColumn<S,T> {
    this.cellValueFactory = callback
    return this
}

fun <S,T> TableColumn<S,T>.cellValueByDefault(): TableColumn<S,T> {
    this.cellValueFactory = Callback { SimpleObjectProperty(it.value as T) }
    return this
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
            forEach{ addAll(it.children(recursive)) }
    }
}

fun <S,T> TableColumn<S,T>.setAlign(align: Pos): TableColumn<S,T> {
    this.style = "${this.style};-fx-alignment:${align.name}"
    return this
}