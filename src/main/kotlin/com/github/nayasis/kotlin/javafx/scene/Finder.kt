package com.github.nayasis.kotlin.javafx.scene

import javafx.scene.Node
import tornadofx.*

fun Node.root(): Node {
    var curr = this
    while( true ) {
        if( curr.parent == null ) return curr
        curr = curr.parent
    }
}

fun Node.children( predicate: (Node) -> Boolean ): List<out Node> {
    val children = mutableListOf<Node>()
    getChildList()?.forEach{
        if( predicate(it) )
            children.add(it)
        children.addAll( it.children(predicate) )
    }
    return children
}

