package com.github.nayasis.sample.fxml.tableview

import com.github.nayasis.kotlin.basica.reflection.Reflector
import com.github.nayasis.kotlin.javafx.control.tableview.column.cellValue
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TableView.*
import javafx.scene.layout.AnchorPane
import javafx.scene.paint.Color
import tornadofx.*

class DemoTableView: View() {

    override val root: AnchorPane by fxml("/views/TableView.fxml")

    private val data = SortedFilteredList(listOf(
        Person("nayasis", 45),
        Person("jake", 9),
        Person("suuny", null),
    ).asObservable())

    private val main: TableView<Person> by fxid()
    private val colName: TableColumn<Person,String> by fxid()
    private val colAge: TableColumn<Person,Int> by fxid()

    init {
        initTable()
    }

    private fun initTable() {

//        val colName = main.findColumnBy<Person,String>("colName")
//        val colAge = main.findColumnBy<Person,Int>("colAge")

        main.columnResizePolicy = CONSTRAINED_RESIZE_POLICY

        colName.cellValue(Person::name)
        colAge.cellValue(Person::age).cellFormat {
            style {
                backgroundColor += if( it > 10 ) Color.RED else Color.GREEN
                textFill = Color.LIGHTCYAN
            }
            text = it.toString()
        }

        data.bindTo( main )

        log.info( ">> properties :\n${Reflector.toJson(this.properties)}")

    }

}

data class Person (
    val name: String? = null,
    val age: Int? = null,
)

class DemoApp: App(DemoTableView::class)

fun main(args: Array<String> ) {
    launch<DemoApp>( args )
}