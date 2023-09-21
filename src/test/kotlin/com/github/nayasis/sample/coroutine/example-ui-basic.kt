package com.github.nayasis.sample.coroutine

import com.github.nayasis.kotlin.javafx.geometry.Insets
import javafx.application.Application
import javafx.event.EventHandler
import javafx.geometry.Pos.BOTTOM_RIGHT
import javafx.geometry.Pos.CENTER
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.text.Text
import javafx.stage.Stage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun main(vararg args: String) {
    Application.launch(ExampleApp::class.java, *args)
}

class ExampleApp: Application() {

    val hello = Text("Hello World!").apply {
        fill = Color.valueOf("#C0C0C0")
    }

    val fab = Circle(20.0, Color.valueOf("#FF4081"))

    val root = StackPane().apply {
        children.addAll(hello, fab)
        StackPane.setAlignment(hello, CENTER)
        StackPane.setAlignment(fab, BOTTOM_RIGHT)
        StackPane.setMargin(fab, Insets(15))
    }

    override fun start(stage: Stage) {
        stage.apply {
            title = "Example"
            scene = Scene(root, 240.0, 380.0).apply {
                fill = Color.valueOf("#303030")
            }
            show()
            setup(hello,fab)
        }

    }

}

//fun setup(hello: Text, fab: Circle) {
//    val job = GlobalScope.launch(Dispatchers.Main) {
//        for(i in 10 downTo 1) {
//            hello.text = "Countdown $i"
//            delay(500)
//        }
//        hello.text = "Done !"
//    }
//    fab.setOnMouseClicked {
//        job.cancel()
//    }
//}

fun setup(hello: Text, fab: Circle) {
//    fab.setOnMouseClicked {
//        GlobalScope.launch(Dispatchers.Main) {
//            for(i in 10 downTo 1) {
//                hello.text = "Countdown $i"
//                delay(500)
//            }
//            hello.text = "Done !"
//        }
//    }
    var status = "none"
    GlobalScope.launch(Dispatchers.Main) {
        var counter = 0
        while(true) {
            hello.text = "${++counter} : $status"
            delay(100)
        }
    }

    var x = 1
    fab.onClick {
        status = "fib($x) = ${fib(x)}"
        x++
    }

}

fun fib(x: Int): Int = if(x <= 1) x else fib(x-1) + fib(x-2)

fun Node.onClick(action: suspend (event: MouseEvent) -> Unit) {
    val eventActor = GlobalScope.actor<MouseEvent>(Dispatchers.IO, capacity= Channel.CONFLATED) {
        for(event in channel) action(event)
    }
    onMouseClicked = EventHandler { event ->
        eventActor.trySend(event)
    }
}

