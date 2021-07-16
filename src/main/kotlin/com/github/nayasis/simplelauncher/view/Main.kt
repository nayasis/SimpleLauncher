package com.github.nayasis.simplelauncher.view

import com.github.nayasis.kotlin.javafx.stage.Localizator
import com.github.nayasis.simplelauncher.jpa.entity.Link
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.layout.AnchorPane
import tornadofx.View
import tornadofx.remainingWidth
import tornadofx.smartResize

class Main: View() {

    override val root: AnchorPane by fxml("/view/main/main.fxml")

    val tableMain: TableView<Link> by fxid()
    val colGroup: TableColumn<Link,String> by fxid()
    val colTitle: TableColumn<Link,String> by fxid()
    val colLastUsedDt: TableColumn<Link,String> by fxid()
    val colExecCount: TableColumn<Link,String> by fxid()

    init {
        Localizator().set(root)
    }

}