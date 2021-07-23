package com.github.nayasis.simplelauncher.view

import com.github.nayasis.kotlin.basica.core.extention.ifNull
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
import com.github.nayasis.simplelauncher.service.LinkExecutor
import com.github.nayasis.simplelauncher.service.LinkService
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.input.TransferMode
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import mu.KotlinLogging
import tornadofx.*
import java.io.File
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

class Main: View() {

    val linkRepository: LinkRepository by di()
    val linkService: LinkService by di()
    val linkExecutor: LinkExecutor by di()

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
    val buttonSave: Button by fxid()
    val buttonDelete: Button by fxid()
    val buttonChangeIcon: Button by fxid()
    val buttonOpenFolder: Button by fxid()
    val buttonCopyFolder: Button by fxid()
    val buttonDuplicate: Button by fxid()

    val descGridPane: GridPane by fxid()
    val descGroupName: TextField by fxid()
    val descShowConsole: CheckBox by fxid()
    val descTitle: TextField by fxid()
    val descDescription: TextArea by fxid()
    val descIcon: ImageView by fxid()
    val descExecPath: TextField by fxid()
    val descArg: TextField by fxid()
    val descCmdPrefix: TextField by fxid()
    val descCmdNext: TextArea by fxid()
    val descCmdPrev: TextArea by fxid()

    val labelStatus: Label by fxid()
    val labelCmd: Label by fxid()

    var detail: Link? = null

    val links = SortedFilteredList(mutableListOf<Link>().asObservable())

    init {
        Localizator().set(root)
        initShortcut()
        initTable()
    }

    private fun initTable() {

        colGroup.cellValue(Link::group)
        colTitle.cellValueByDefault().cellFormat {
            graphic = hbox {
                imageview {
                    image = it.getIconAsImage()
                    HBox.setMargin( this, Insets(0,0,0,2) )
                }
                label {
                    text = it.title ?: ""
                    HBox.setMargin( this, Insets(0,0,0,5) )
                }
            }
            setOnDragOver { event -> if( event.dragboard.hasFiles() ) {
                event.acceptTransferModes(TransferMode.LINK)
            }}
            setOnDragDropped { event->
                val dragboard = event.dragboard
                if( dragboard.hasFiles() ) {
                    val link = tableRow.item as Link
                    linkExecutor.run(link,dragboard.files)
                }
                event.isDropCompleted = true
                event.consume()
            }
        }

        colTitle.setComparator { o1, o2 -> o1.title.ifNull{""}.compareTo(o2.title.ifNull{""}) }

        colLastUsedDt.cellValue(Link::lastExecDate).cellFormat {
            text = it.toFormat("YYYY-MM-DD HH:MI:SS")
        }
        colExecCount.cellValue(Link::executeCount).setAlign(Pos.CENTER_RIGHT)

        links.bindTo(tableMain)

        colGroup.remainingWidth()
        colTitle.remainingWidth()
        tableMain.smartResize()
        tableMain.selectionModel.selectionMode = SelectionMode.SINGLE

        tableMain.setOnMouseClicked { event ->
            if(event.button == MouseButton.PRIMARY && event.clickCount > 1) {
                tableMain.selectedItem?.let { link -> linkExecutor.run(link) }
            }
        }

        tableMain.selectionModel.selectedItems.addListener { change: ListChangeListener.Change<out Link?> ->
            if (change.list.isEmpty()) return@addListener
            drawDetail(change.list[0])
        }

        tableMain.setOnKeyPressed { event ->
            when(event.code) {
                KeyCode.ENTER -> {
                    tableMain.selectedItem?.let { linkExecutor.run(it) }
                }
                KeyCode.DELETE -> {
                    tableMain.selectedItem?.let { linkService.delete(it) }
                }
                KeyCode.ESCAPE -> {
                    TODO("Focus to keyword input")
                }
                KeyCode.TAB -> {
//                    TODO( "traverse to descGroupName")
                }
            }
        }

        readData()

        logger.debug { ">> done initialize" }

    }

    private fun initShortcut() {

        menuImportData.setOnAction {
            linkService.openImportFilePicker()?.let { file ->
                linkService.importData(file)
                readData()
                Dialog.alert( "msg.info.009".message().format(file) )
            }
        }

        menuExportData.setOnAction {
            linkService.openExportFilePicker()?.let { file ->
                linkService.exportData(file!!)
                Dialog.alert( "msg.info.010".message().format(file) )
            }
        }

        menuDeleteAll.setOnAction {
            linkService.deleteAll()
            links.clear()
            clearDetailView()
        }

        buttonOpenFolder.setOnAction { tableMain.selectedItem?.let { linkExecutor.openFolder(it) } }

        buttonCopyFolder.setOnAction { tableMain.selectedItem?.let { linkExecutor.copyFolder(it) } }

    }

    private fun clearDetailView() = drawDetail(Link())

    fun readData() {
        links.addAll(linkRepository.findAllByOrderByTitle())
        printSearchResult()
    }

    fun drawDetail(link: Link?) {

        if( link == null || (link.id != null && detail?.id == link.id) ) return

        detail = link

        with(detail!!) {
            descTitle.text = title
            descShowConsole.isSelected = showConsole
            descGroupName.text = group
            descDescription.text = description
            descExecPath.text = path
            descArg.text = argument
            descCmdPrefix.text = commandPrefix
            descCmdPrev.text = commandPrev
            descCmdNext.text = commandNext
            descIcon.image = getIconAsImage()
        }

        buttonDelete.isDisable = false
        buttonDuplicate.isDisable = false
        buttonSave.isDisable = false

    }

    fun printCommand(command: String? = null) {
        labelCmd.text = command ?: ""
    }

    fun printStatus(status: String? = null) {
        labelStatus.text = status ?: ""
    }

    fun printSearchResult() {
        printStatus("msg.info.005".message().format(links.sortedItems.size, links.size) )
    }

}