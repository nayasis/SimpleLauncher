package com.github.nayasis.kotlin.javafx.stage

import com.github.nayasis.kotlin.basica.model.Messages
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.Pane
import java.util.*

class Localizator (
    val node: Node?,
    val locale: Locale = Locale.getDefault()
) {

    init {
        set(node)
    }

    fun set(node: Node?) {
        when (node) {
            null            -> return
            is MenuBar      -> set(node)
            is Label        -> set(node)
            is TextField    -> set(node)
            is CheckBox     -> set(node)
            is Button       -> set(node)
            is TableView<*> -> set(node)
            is SplitPane    -> set(node)
            is TabPane      -> set(node)
            is ScrollPane   -> set(node)
            is Pane         -> set(node)
        }
    }

    private fun set(pane: Pane) {
        for (node in pane.children) {
            set(node)
        }
    }

    private fun set(tableView: TableView<*>) {
        for (column in tableView.columns) {
            set(column as TableColumn<*,*>)
        }
    }

    private fun set(tableColumn: TableColumn<*, *>) {
        tableColumn.text = convert(tableColumn.text)
        for (subColumn in tableColumn.columns) {
            set(subColumn)
        }
    }

    private fun set(splitPane: SplitPane) {
        for (node in splitPane.items) {
            set(node)
        }
    }

    private fun set(scrollPane: ScrollPane) {
        set(scrollPane.content)
    }

    private fun set(tabPane: TabPane) {
        for (node in tabPane.tabs) {
            set(node)
        }
    }

    private fun set(tab: Tab) {
        tab.text = convert(tab.text)
        if (tab.tooltip != null) {
            tab.tooltip.text = convert(tab.tooltip.text)
        }
        set(tab.content)
    }

    private fun set(button: Button) {
        button.text = convert(button.text)
        set(button.tooltip)
    }

    private fun set(tooltip: Tooltip?) {
        if (tooltip != null) {
            tooltip.text = convert(tooltip.text)
        }
    }

    private fun set(checkBox: CheckBox) {
        checkBox.text = convert(checkBox.text)
    }

    private fun set(textField: TextField) {
        textField.text = convert(textField.text)
    }

    private fun set(label: Label) {
        label.text = convert(label.text)
        set(label.tooltip)
    }

    private fun set(menu: Menu) {
        menu.text = convert(menu.text)
        for (menuItem in menu.items) {
            set(menuItem)
        }
    }

    private fun set(menuBar: MenuBar) {
        for (menu in menuBar.menus) {
            set(menu)
        }
    }

    private fun set(menuItem: MenuItem) {
        menuItem.text = convert(menuItem.text)
        if (menuItem is Menu) {
            set(menuItem)
        }
    }


    private fun convert(text: String): String {
        if ( text.isEmpty() || ! text.startsWith("%") ) return text
        return Messages[locale, text.substring(1)]
    }

}