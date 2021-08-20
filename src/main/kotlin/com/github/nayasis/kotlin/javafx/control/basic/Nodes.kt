package com.github.nayasis.kotlin.javafx.control.basic

import javafx.css.Styleable
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.ScrollPane
import javafx.scene.control.SplitPane
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.ToolBar
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import mu.KotlinLogging
import tornadofx.getChildList

private val log = KotlinLogging.logger{}

fun Node.root(): Node {
    var curr = this
    while( true ) {
        if( curr.parent == null ) return curr
        curr = curr.parent
    }
}

val Node.allChildren: List<Node>
    get() {
        return HashSet<Node>().let{
            gatherChildren(this,it)
            it.remove(this)
            it
        }.toList()
    }

private fun gatherChildren(target: Node?, targets: HashSet<Node>) {
    if(target == null) return
    targets.add(target)
    target.getChildList()?.forEach { gatherChildren(it,targets) }
}


val Styleable.allStyleables: List<Styleable>
    get() {
        return HashSet<Styleable>().let {
            gatherStyleables(this,it)
            it
        }.toList()
    }

private fun gatherStyleables(target: Styleable?, targets: HashSet<Styleable>) {
    if(target == null) return
    targets.add(target)
    when (target) {
        is SplitPane         -> target.items.forEach{gatherStyleables(it,targets)}
        is ToolBar           -> target.items.forEach{gatherStyleables(it,targets)}
        is Pane              -> target.children.forEach{gatherStyleables(it,targets)}
        is Group             -> target.children.forEach{gatherStyleables(it,targets)}
        is HBox              -> target.children.forEach{gatherStyleables(it,targets)}
        is VBox              -> target.children.forEach{gatherStyleables(it,targets)}
        is TabPane           -> target.tabs.forEach{gatherStyleables(it,targets)}
        is ScrollPane        -> target.content.allStyleables.forEach{gatherStyleables(it,targets)}
        is Tab               -> target.content.allStyleables.forEach{gatherStyleables(it,targets)}
        is MenuBar           -> target.menus.forEach{gatherStyleables(it,targets)}
        is Menu              -> target.items.forEach{gatherStyleables(it,targets)}
        is TableView<*>      -> target.columns.forEach{gatherStyleables(it,targets)}
        is TableColumn<*, *> -> target.columns.forEach{gatherStyleables(it,targets)}
    }
}