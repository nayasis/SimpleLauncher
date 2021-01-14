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

