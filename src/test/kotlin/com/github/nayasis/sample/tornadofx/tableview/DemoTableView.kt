package com.github.nayasis.sample.tornadofx.tableview

import com.github.nayasis.sample.tornadofx.fxml.tableview.Person
import tornadofx.App
import tornadofx.View
import tornadofx.asObservable
import tornadofx.launch
import tornadofx.readonlyColumn
import tornadofx.resizeColumnsToFitContent
import tornadofx.tableview
import tornadofx.vbox

fun main(args: Array<String>) {
    launch<DemoApp>(args)
}

class DemoApp: App( DemoTableView::class)

class DemoTableView: View() {

    val persons = listOf(
        Person("nayasis", 45),
        Person("jake", 10),
    ).asObservable()

    override val root = vbox {
        tableview( persons ) {
            readonlyColumn("Name", Person::name )
            readonlyColumn("Age",  Person::age )
            resizeColumnsToFitContent()
        }
    }

}

data class Person (
    val name: String? = null,
    val age: Int? = null,
)