package com.github.nayasis.simplelauncher.view

import com.github.nayasis.kotlin.basica.core.extention.ifEmpty
import com.github.nayasis.kotlin.basica.core.extention.ifNull
import com.github.nayasis.kotlin.basica.core.localdate.toFormat
import com.github.nayasis.kotlin.basica.core.string.message
import com.github.nayasis.kotlin.javafx.control.tableview.column.cellValue
import com.github.nayasis.kotlin.javafx.control.tableview.column.cellValueByDefault
import com.github.nayasis.kotlin.javafx.control.tableview.column.setAlign
import com.github.nayasis.kotlin.javafx.control.tableview.focused
import com.github.nayasis.kotlin.javafx.control.tableview.select
import com.github.nayasis.kotlin.javafx.geometry.Insets
import com.github.nayasis.kotlin.javafx.misc.Desktop
import com.github.nayasis.kotlin.javafx.misc.set
import com.github.nayasis.kotlin.javafx.stage.Dialog
import com.github.nayasis.kotlin.javafx.stage.Localizator
import com.github.nayasis.simplelauncher.common.Context
import com.github.nayasis.simplelauncher.common.ICON_NEW
import com.github.nayasis.simplelauncher.jpa.entity.Link
import com.github.nayasis.simplelauncher.jpa.repository.LinkRepository
import com.github.nayasis.simplelauncher.service.LinkExecutor
import com.github.nayasis.simplelauncher.service.LinkService
import javafx.beans.value.ObservableValue
import javafx.collections.ListChangeListener
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCode.*
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.TransferMode
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import mu.KotlinLogging
import tornadofx.SortedFilteredList
import tornadofx.View
import tornadofx.asObservable
import tornadofx.hbox
import tornadofx.imageview
import tornadofx.label
import tornadofx.remainingWidth
import tornadofx.selectedItem
import tornadofx.smartResize
import java.io.File
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

class Main: View("application.title".message()) {

    val linkRepository: LinkRepository by di()
    val linkService: LinkService by di()
    val linkExecutor: LinkExecutor by di()

    override val root: AnchorPane by fxml("/view/main/main.fxml")

    val tableMain: TableView<Link> by fxid()
    val colGroup: TableColumn<Link,String> by fxid()
    val colTitle: TableColumn<Link,Link> by fxid()
    val colLastUsedDt: TableColumn<Link,LocalDateTime> by fxid()
    val colExecCount: TableColumn<Link,Int> by fxid()

    val vboxTop: VBox by fxid()
    val menubarTop: MenuBar by fxid()
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
    val buttonCopy: Button by fxid()

    val descGridPane: GridPane by fxid()
    val descGroupName: TextField by fxid()
    val descShowConsole: CheckBox by fxid()
    val descEachExecution: CheckBox by fxid()
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
        Localizator(root)
        initEvent()
        initTable()
    }

    private fun initTable() {

        colGroup.cellValue(Link::group)
        colTitle.cellValueByDefault().setAlign(Pos.CENTER_LEFT).cellFormat {
            graphic = hbox {
                imageview {
                    image = it.getIconImage()
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
                event.dragboard.let {
                    if( it.hasFiles() ) {
                        val link = tableRow.item as Link
                        linkExecutor.run(link,it.files)
                    }
                }
                event.isDropCompleted = true
                event.consume()
            }
        }

        colTitle.setComparator { o1, o2 -> o1.title.ifNull{""}.compareTo(o2.title.ifNull{""}) }

        colLastUsedDt.cellValue(Link::lastExecDate).setAlign(Pos.CENTER).cellFormat {
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

        tableMain.focusedProperty().addListener { _, _, newVal ->
            if( newVal == true && tableMain.selectionModel.focusedIndex <= 0 ) {
                tableMain.selectionModel.selectFirst()
            }
        }

        tableMain.selectionModel.selectedItems.addListener { change: ListChangeListener.Change<out Link?> ->
            if (change.list.isEmpty()) return@addListener
            drawDetail(change.list[0])
        }

        tableMain.setOnKeyPressed { event ->
            when(event.code) {
                ENTER -> {
                    tableMain.selectedItem?.let { linkExecutor.run(it) }
                }
                DELETE -> {
                    if(event.isShiftDown)
                        tableMain.selectedItem?.let { deleteLink(it) }
                }
                ESCAPE -> {
                    inputKeyword.requestFocus()
                }
                TAB -> {
//                    TODO( "traverse to descGroupName")
                }
            }
        }

        readLinks()

        logger.debug { ">> done initialize" }

    }

    private fun initEvent() {

        // global shortcut
        root.setOnKeyPressed { e ->
            if( e.isControlDown ) {
                when(e.code) {
                    S -> buttonSave       .let { if(!it.isDisable) it.fire() }
                    D -> buttonDelete     .let { if(!it.isDisable) it.fire() }
                    C -> buttonCopy       .let { if(!it.isDisable) it.fire() }
                    I -> buttonChangeIcon .let { if(!it.isDisable) it.fire() }
                    N -> buttonNew        .let { if(!it.isDisable) it.fire() }
                    O -> buttonOpenFolder .let { if(!it.isDisable) it.fire() }
                    F -> buttonCopyFolder .let { if(!it.isDisable) it.fire() }
                }
            }
        }

        menuImportData.setOnAction {
            linkService.openImportFilePicker()?.let { file ->
                linkService.importData(file)
                readLinks()
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
            clearDetail()
        }

        menuitemViewDesc.selectedProperty().addListener { _, _, value ->
            showDetail(value)
        }

        menuitemViewMenuBar.selectedProperty().addListener { _, _, value ->
            showMenubar(value)
        }

        menuitemAlwaysOnTop.selectedProperty().addListener { _, _, value ->
            Context.main.primaryStage.isAlwaysOnTop = value
        }

        buttonSave.setOnAction { saveDetail() }
        buttonDelete.setOnAction { detail?.let{ deleteLink(it) } }
        buttonCopy.setOnAction { copyDetail() }
        buttonNew.setOnAction { createDetail() }
        buttonOpenFolder.setOnAction { tableMain.selectedItem?.let { linkExecutor.openFolder(it) } }
        buttonCopyFolder.setOnAction { tableMain.selectedItem?.let { linkExecutor.copyFolder(it) } }

        buttonChangeIcon.setOnAction { changeIcon() }
        descIcon.setOnMouseClicked { e ->
            if( e.isPrimaryButtonDown && e.clickCount > 1 )
                changeIcon()
        }
        descIcon.setOnDragDropped { e ->
            e.dragboard.files.firstOrNull()?.let { changeIcon(it) }
        }
        descExecPath.setOnDragDropped { e ->
            e.dragboard.files.firstOrNull()?.let {
                detail?.setPath(it)
                descExecPath.text = detail?.path
                changeIcon(it)
            }
        }

        labelCmd.setOnMouseClicked { event ->
            if(event.button == MouseButton.PRIMARY && event.clickCount > 1) {
                Desktop.clipboard.set(labelCmd.text)
            }
        }

        descGridPane.children.filterIsInstance<TextArea>().forEach { tabPressed(it) }

        // 상세내역 변경시 버튼 컨트롤
        val listener: (observable: ObservableValue<*>, oldValue: Any?, newValue: Any?) -> Unit =
            { _,_,_ -> buttonSave.isDisable = false }
        descGridPane.children.filterIsInstance<TextInputControl>().forEach {
            it.textProperty().addListener(listener)
        }
        descIcon.imageProperty().addListener(listener)

    }

    private fun changeIcon() {
        linkService.openIconFilePicker()?.let { changeIcon(it) }
    }

    private fun changeIcon(file: File) {
        detail?.setIcon(file)?.let { icon ->
            descIcon.image = icon
        }
    }

    private fun tabPressed(textArea: TextArea) {
        textArea.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
            if( event.code != TAB || event.isShiftDown || event.isControlDown ) return@addEventFilter
            event.consume()
            (event.source as Node).fireEvent( KeyEvent(
                event.source,
                event.target,
                event.eventType,
                event.character,
                event.text,
                event.code,
                event.isShiftDown,
                true,
                event.isAltDown,
                event.isMetaDown
            ))
        }
    }

    fun showDetail(show: Boolean) {
        (tableMain.parent as HBox).children.also {
            if(show) {
                if( descGridPane !in it )
                    it.add(descGridPane)
            } else {
                it.remove(descGridPane)
            }
        }
    }

    fun showMenubar(show: Boolean) {
        vboxTop.children.also {
            if(show) {
                if(menubarTop !in it)
                    it.add(0,menubarTop)
            } else {
                it.remove(menubarTop)
            }
        }
    }

    fun readLinks() {
        links.apply {
            clear()
            addAll(linkRepository.findAllByOrderByTitle())
        }
        printSearchResult()
    }

    private fun clearDetail() {
        drawDetail(Link())
        buttonSave.isDisable = false
    }

    fun drawDetail(link: Link?) {
        if( link == null || (link.id != 0L && detail?.id == link.id) ) return
        detail = link
        with(detail!!) {
            descTitle.text               = title
            descShowConsole.isSelected   = showConsole
            descEachExecution.isSelected = eachExecution
            descGroupName.text           = group
            descDescription.text         = description
            descExecPath.text            = path
            descArg.text                 = argument
            descCmdPrefix.text           = commandPrefix
            descCmdPrev.text             = commandPrev
            descCmdNext.text             = commandNext
            descIcon.image               = getIconImage()
        }
        buttonDelete.isDisable = false
        buttonCopy.isDisable = false
        buttonSave.isDisable = true
    }

    fun deleteLink(link: Link?) {

        if( link == null ) return

        val summary = if( ! link.group.isNullOrEmpty() ) "[${link.group}] ${link.title}" else "${link.title}"

        if( ! Dialog.confirm("msg.confirm.001".message().format(summary)) ) return

        val prev = tableMain.focused()

        linkService.delete(link)
        links.remove(link)

        clearDetail()
        tableMain.select(prev.row)
        printSearchResult()

    }

    fun saveDetail() {
        detail?.let {
            val isNew = it.id == 0L
            it.title         = descTitle.text?.trim()
            it.showConsole   = descShowConsole.isSelected
            it.eachExecution = descEachExecution.isSelected
            it.group         = descGroupName.text?.trim()
            it.description   = descDescription.text?.trim()
            it.path          = descExecPath.text?.trim()
            it.argument      = descArg.text?.trim()
            it.commandPrefix = descCmdPrefix.text?.trim()
            it.commandPrev   = descCmdPrev.text
            it.commandNext   = descCmdNext.text
            it.setIcon(descIcon.image)
            linkService.save(it)
            if(isNew) {
                links.add(it)
                tableMain.refresh()
                printSearchResult()
            } else {
                tableMain.refresh()
            }
            printStatus("msg.info.013".message())
            buttonDelete.isDisable = false
            buttonCopy.isDisable = false
            buttonSave.isDisable = true
        }
    }

    fun copyDetail() {
        if( detail == null || detail?.id == 0L ) return
        drawDetail( detail!!.clone().apply { id = 0L } )
        descTitle.requestFocus()
        printStatus("msg.info.014".message())
        buttonDelete.isDisable = true
        buttonCopy.isDisable = true
        buttonSave.isDisable = false
    }

    fun createDetail() {
        drawDetail(Link().apply { icon = ICON_NEW })
        descGroupName.requestFocus()
        printStatus("msg.info.015".message())
        buttonDelete.isDisable = true
        buttonCopy.isDisable = true
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