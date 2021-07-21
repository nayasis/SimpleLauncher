package com.github.nayasis.simplelauncher.view

import com.github.nayasis.kotlin.basica.core.localdate.toFormat
import com.github.nayasis.kotlin.basica.core.string.message
import com.github.nayasis.kotlin.javafx.control.tableview.column.cellValue
import com.github.nayasis.kotlin.javafx.control.tableview.column.cellValueByDefault
import com.github.nayasis.kotlin.javafx.control.tableview.column.setAlign
import com.github.nayasis.kotlin.javafx.geometry.Insets
import com.github.nayasis.kotlin.javafx.misc.Images
import com.github.nayasis.kotlin.javafx.stage.Dialog
import com.github.nayasis.kotlin.javafx.stage.Localizator
import com.github.nayasis.simplelauncher.jpa.entity.Link
import com.github.nayasis.simplelauncher.jpa.repository.LinkRepository
import com.github.nayasis.simplelauncher.service.LinkService
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import mu.KotlinLogging
import org.apache.commons.cli.CommandLine
import tornadofx.View
import tornadofx.hbox
import tornadofx.imageview
import tornadofx.label
import tornadofx.remainingWidth
import tornadofx.smartResize
import tornadofx.toProperty
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

private const val FILE_EXT_DESC = "Data File (*.sl)"
private const val FILE_EXT = "*.sl"

class Main: View() {

    val linkRepository: LinkRepository by di()
    val linkService: LinkService by di()

    override val root: AnchorPane by fxml("/view/main/main.fxml")

    val tableMain: TableView<Link> by fxid()
    val colGroup: TableColumn<Link,String> by fxid()
    val colTitle: TableColumn<Link,Link> by fxid()
    val colLastUsedDt: TableColumn<Link,LocalDateTime> by fxid()
    val colExecCount: TableColumn<Link,Int> by fxid()

    val vboxTop: AnchorPane by fxid()
    val menuitemViewDesc: CheckMenuItem by fxid()
    val menuitemViewMenuBar: CheckMenuItem by fxid()
    val menuitemAlwaysOnTop: CheckMenuItem by fxid()
    val menuItemHelp: MenuItem by fxid()
    val menuImportData: MenuItem by fxid()
    val menuExportData: MenuItem by fxid()
    val menuDeleteAll: MenuItem by fxid()

    val inputKeyword: TextField by fxid()
    val inputGroup: TextField by fxid()

    val buttonNew: Button by fxid()
    val buttonCopy: Button by fxid()
    val buttonSave: Button by fxid()
    val buttonDelete: Button by fxid()
    val buttonChangeIcon: Button by fxid()
    val buttonOpenFolder: Button by fxid()

    val descGridPane: GridPane by fxid()
    val descGroupName: TextField by fxid()
    val descShowConsole: CheckBox by fxid()
    val descTitle: TextField by fxid()
    val descDescription: TextArea by fxid()
    val descIcon: ImageView by fxid()
    val descExecPath: TextField by fxid()
    val descExecOption: TextField by fxid()
    val descExecOptionPrefix: TextField by fxid()
    val descCmdNext: TextArea by fxid()
    val descCmdPrev: TextArea by fxid()

    val labelStatus: Label by fxid()
    val labelCmd: Label by fxid()

    init {
        Localizator().set(root)
        initShortcut()
        initTable()
    }

    private fun initTable() {

        logger.debug { ">> start initialize" }

        colGroup.remainingWidth()
        colTitle.remainingWidth()
        tableMain.smartResize()
        tableMain.selectionModel.selectionMode = SelectionMode.SINGLE

        colGroup.cellValue(Link::group)
        colTitle.cellValueByDefault().cellFormat {
            graphic = hbox {
                imageview {
                    image = Images.toImage(it.icon)
                    HBox.setMargin( this, Insets(0,0,0,2) )
                }
                label {
                    text = it.title ?: ""
                    HBox.setMargin( this, Insets(0,0,0,5) )
                }
            }
        }

        colLastUsedDt.cellValue(Link::lastExecDate).cellFormat {
            text = it.toFormat("YYYY-MM-DD HH:MI:SS")
        }
        colExecCount.cellValue(Link::executeCount).setAlign(Pos.CENTER_RIGHT)

        readData()

    }

    private fun initShortcut() {

        menuImportData.setOnAction {
            val file = linkService.openImportFilePicker().also { if(it==null) return@setOnAction }!!
            linkService.importData(file)
            readData()
            Dialog.alert( "msg.info.009".message().format(file) )
        }

        menuExportData.setOnAction {
            val file = linkService.openExportFilePicker().also { if(it==null) return@setOnAction }!!
            linkService.exportData(file!!)
            Dialog.alert( "msg.info.010".message().format(file) )
        }

        menuDeleteAll.setOnAction {
            linkService.deleteAll()
            tableMain.items.clear()
            clearDetailView()
        }

    }

    private fun clearDetailView() {

    }

    fun readData() {
        val links = linkRepository.findAllByOrderByTitle()
        tableMain.items.addAll(FXCollections.observableArrayList(links))
    }



}