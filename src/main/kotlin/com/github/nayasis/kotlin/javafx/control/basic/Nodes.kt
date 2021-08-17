package com.github.nayasis.kotlin.javafx.control.basic

import javafx.scene.Node
import tornadofx.getChildList

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

private fun gatherChildren(node: Node?, nodes: HashSet<Node>) {
    if(node == null) return
    nodes.add(node)
    node.getChildList()?.forEach { gatherChildren(it,nodes) }
}