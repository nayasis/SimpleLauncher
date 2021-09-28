package com.github.nayasis.kotlin.javafx.control.tableview.column

import javafx.beans.property.BooleanProperty
import javafx.beans.value.ObservableValue
import javafx.geometry.Pos
import javafx.scene.control.CheckBox
import javafx.scene.control.TableCell


class CheckBoxTableCell<S,T> : TableCell<S,T>() {

    private val checkBox: CheckBox = CheckBox()
    private var value: ObservableValue<T>? = null

    init {
        checkBox.alignment = Pos.CENTER
        alignment = Pos.CENTER
        graphic = checkBox
    }

    public override fun updateItem(item: T, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty) {
            setText(null)
            setGraphic(null)
        } else {
            setGraphic(checkBox)
            if (value is BooleanProperty) {
                checkBox.selectedProperty().unbindBidirectional(value as BooleanProperty?)
            }
            value = tableColumn.getCellObservableValue(index)
            if (value is BooleanProperty) {
                checkBox.selectedProperty().bindBidirectional(value as BooleanProperty?)
            }
        }
    }

}