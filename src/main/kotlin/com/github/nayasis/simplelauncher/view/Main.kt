@file:Suppress("MemberVisibilityCanBePrivate")

package com.github.nayasis.simplelauncher.view

import com.github.nayasis.kotlin.basica.core.extention.ifNull
import com.github.nayasis.kotlin.basica.core.localdate.between
import com.github.nayasis.kotlin.basica.core.localdate.toFormat
import com.github.nayasis.kotlin.basica.core.string.message
import com.github.nayasis.kotlin.javafx.control.basic.allChildren
import com.github.nayasis.kotlin.javafx.control.basic.repack
import com.github.nayasis.kotlin.javafx.control.tableview.column.cellValue
import com.github.nayasis.kotlin.javafx.control.tableview.column.cellValueByDefault
import com.github.nayasis.kotlin.javafx.control.tableview.column.setAlign
import com.github.nayasis.kotlin.javafx.control.tableview.focus
import com.github.nayasis.kotlin.javafx.control.tableview.focused
import com.github.nayasis.kotlin.javafx.control.tableview.select
import com.github.nayasis.kotlin.javafx.control.tableview.visibleRows
import com.github.nayasis.kotlin.javafx.geometry.Insets
import com.github.nayasis.kotlin.javafx.misc.Desktop
import com.github.nayasis.kotlin.javafx.misc.set
import com.github.nayasis.kotlin.javafx.property.StageProperty
import com.github.nayasis.kotlin.javafx.stage.Dialog
import com.github.nayasis.kotlin.javafx.stage.Localizator
import com.github.nayasis.simplelauncher.common.Context
import com.github.nayasis.simplelauncher.common.ICON_NEW
import com.github.nayasis.simplelauncher.jpa.entity.Link
import com.github.nayasis.simplelauncher.jpa.repository.LinkRepository
import com.github.nayasis.simplelauncher.service.ConfigService
import com.github.nayasis.simplelauncher.service.LinkExecutor
import com.github.nayasis.simplelauncher.service.LinkService
import com.github.nayasis.simplelauncher.service.TextMatcher
import javafx.beans.value.ObservableValue
import javafx.collections.ListChangeListener
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.input.DragEvent
import javafx.scene.input.KeyCode.*
import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyEvent.KEY_PRESSED
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
import tornadofx.onChange
import tornadofx.runLater
import tornadofx.selectedItem
import java.io.File
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import kotlin.concurrent.timer

private val logger = KotlinLogging.logger {}

class Main: View("application.title".message()) {

    val linkRepository: LinkRepository by di()
    val linkService: LinkService by di()
    val linkExecutor: LinkExecutor by di()

    override val root: AnchorPane by fxml("/view/main/main.fxml")

    val tableMain: TableView<Link> by fxid()
    val colGroup: TableColumn<Link,String> by fxid()
    val colTitle: TableColumn<Link,Link> by fxid()
    val colLastUsedDt: TableColumn<Link,LocalDateTime?> by fxid()
    val colExecCount: TableColumn<Link,Int> by fxid()

    val vboxTop: VBox by fxid()
    val menubarTop: MenuBar by fxid()
    val menuViewDesc: CheckMenuItem by fxid()
    val menuViewMenuBar: CheckMenuItem by fxid()
    val menuShowInputGroup: CheckMenuItem by fxid()
    val menuAlwaysOnTop: CheckMenuItem by fxid()
    val menuHelp: MenuItem by fxid()
    val menuImportData: MenuItem by fxid()
    val menuExportData: MenuItem by fxid()
    val menuDeleteAll: MenuItem by fxid()

    val inputKeyword: TextField by fxid()
    val inputGroup: TextField by fxid()

    val buttonNew: Button by fxid()
    val buttonSave: Button by fxid()
    val buttonDelete: Button by fxid()
    val buttonOpenFolder: Button by fxid()
    val buttonCopyFolder: Button by fxid()
    val buttonCopy: Button by fxid()
    val buttonAddFile: ImageView by fxid()

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

    val keywordMatcher = TextMatcher()
    val groupMatcher = TextMatcher()

    private var lastFocused: Node? = null

    init {
        Localizator(root)
        initEvent()
        initSearchFilter()
        initTable()
    }

    override fun onBeforeShow() {
        ConfigService.stageMain?.let {
            it.excludeKlass.add(Button::class)
            it.bind(currentStage!!)
            menubarTop.repack()
        }
    }

    override fun onUndock() {
        ConfigService.stageMain = StageProperty(currentStage!!)
        ConfigService.save()
    }

    private fun initTable() {

        colGroup.cellValue(Link::group)
        colTitle.cellValueByDefault().cellFormat {
            graphic = hbox {
                imageview {
                    image = it.getIconImage()
                    HBox.setMargin( this, Insets(0,0,0,2) )
                }
                label {
                    text = it.title ?: ""
                    HBox.setMargin( this, Insets(0,0,0,5) )
                }
                alignment = Pos.CENTER_LEFT
            }
            setOnDragOver { fnDraggable(it) }
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

        colLastUsedDt.cellValue(Link::lastExecDate).cellFormat {
            text = it?.toFormat("YYYY-MM-DD HH:MI:SS")
            alignment = Pos.CENTER
        }
        colExecCount.cellValue(Link::executeCount).setAlign(Pos.CENTER_RIGHT)

        links.bindTo(tableMain)

//        colGroup.remainingWidth()
//        colTitle.remainingWidth()
//        tableMain.smartResize()
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
                ENTER -> tableMain.selectedItem?.let { linkExecutor.run(it) }
                ESCAPE -> inputKeyword.requestFocus()
                DELETE -> tableMain.selectedItem?.let{ deleteLink(it) }
                TAB -> {
                    if( ! event.isShiftDown ) {
                        event.consume()
                        (if(lastFocused == null || lastFocused == tableMain) descGroupName else lastFocused)!!.requestFocus()
                    }
                }
            }
        }

        readLinks()

        currentStage?.requestFocus()

        logger.debug { ">> done initialize" }

    }

    private fun initEvent() {

        // global shortcut
        root.setOnKeyPressed { e ->
            if( e.isControlDown ) {
                when(e.code) {
                    S -> buttonSave.let { if(!it.isDisable) it.fire() }
                    D -> buttonDelete.let { if(!it.isDisable) it.fire() }
                    C -> buttonCopy.let { if(!it.isDisable) it.fire() }
                    N -> if( e.isShiftDown) {
                            buttonAddFile.fireEvent(e)
                        } else {
                            buttonNew.let { if(!it.isDisable) it.fire() }
                        }
                    O -> buttonOpenFolder.let { if(!it.isDisable) it.fire() }
                    F -> buttonCopyFolder.let { if(!it.isDisable) it.fire() }
                    I -> changeIcon()
                }
            }
        }

        menuImportData.setOnAction {
            linkService.openImportPicker()?.let { file ->
                linkService.importData(file)
                readLinks()
                Dialog.alert( "msg.info.009".message().format(file) )
            }
        }

        menuExportData.setOnAction {
            linkService.openExportPicker()?.let { file ->
                linkService.exportData(file)
                Dialog.alert( "msg.info.010".message().format(file) )
            }
        }

        menuDeleteAll.setOnAction {
            if(Dialog.confirm("msg.confirm.002".message())) {
                linkService.deleteAll()
                links.clear()
                clearDetail()
            }
        }

        menuViewDesc.selectedProperty().addListener { _, _, show ->
            (tableMain.parent as HBox).children.also {
                if(show && descGridPane !in it) {
                    it.add(descGridPane)
                } else {
                    it.remove(descGridPane)
                }
            }
        }

        menuViewMenuBar.selectedProperty().addListener { _, _, show ->
            menubarTop.let {
                it.isVisible = !show
                it.repack()
            }
        }

        menuShowInputGroup.selectedProperty().addListener{_,_,show ->
            val group = inputGroup.parent as HBox
            (inputKeyword.parent as HBox).children.also {
                if(show && group !in it) {
                    it.add(it.indexOf(inputKeyword) + 1, group)
                    inputGroup.requestFocus()
                } else {
                    it.remove(group)
                    inputGroup.text = ""
                }
            }
        }

        menuAlwaysOnTop.selectedProperty().addListener { _, _, flag ->
            Context.main.primaryStage.isAlwaysOnTop = flag
        }

        menuHelp.setOnAction{
            find<Help>().openWindow()
        }

        buttonSave.setOnAction { saveDetail() }
        buttonDelete.setOnAction { detail?.let{ deleteLink(it) } }
        buttonCopy.setOnAction { copyDetail() }
        buttonNew.setOnAction { createDetail() }
        buttonOpenFolder.setOnAction { tableMain.selectedItem?.let { linkExecutor.openFolder(it) } }
        buttonCopyFolder.setOnAction { tableMain.selectedItem?.let { linkExecutor.copyFolder(it) } }

        descIcon.setOnMouseClicked { e ->
            if( e.button == MouseButton.PRIMARY && e.clickCount > 1 )
                changeIcon()
        }
        descIcon.setOnDragOver { fnDraggable(it) }
        descIcon.setOnDragDropped { e ->
            e.dragboard.files.firstOrNull()?.let {
                changeIcon(it)
            }
        }
        Tooltip("btn.change.icon.tooltip".message()).let {
            Tooltip.install(descIcon,it)
        }
        Tooltip("btn.addfile.tooltip".message()).let {
            Tooltip.install(buttonAddFile,it)
        }

        buttonAddFile
        descExecPath.setOnDragOver { fnDraggable(it) }
        descExecPath.setOnDragDropped { e ->
            e.dragboard.files.firstOrNull()?.let {
                detail?.setPath(it)
                descExecPath.text = detail?.path
                changeIcon(it)
            }
        }

        buttonAddFile.setOnDragOver { fnDraggable(it) }
        buttonAddFile.setOnDragDropped { e ->
            e.dragboard.files.forEach { file ->
                drawDetailForAdd(Link(file))
                currentStage?.requestFocus()
            }
        }
        buttonAddFile.setOnMouseClicked { e ->
            if( e.button == MouseButton.PRIMARY ) {
                linkService.openExecutorPicker()?.let { file ->
                    drawDetailForAdd(Link(file))
                }
            }
        }

        labelCmd.setOnMouseClicked { e ->
            if(e.button == MouseButton.PRIMARY && e.clickCount > 1) {
                Desktop.clipboard.set(labelCmd.text)
            }
        }

        inputKeyword.addEventFilter(KEY_PRESSED) { e ->
            if( e.code == ENTER ) {
                if( tableMain.visibleRows in 1..10 ) {
                    e.consume()
                    tableMain.focus(0)
                    tableMain.selectedItem?.let { link -> linkExecutor.run(link) }
                }
            }
        }

        inputGroup.addEventFilter(KEY_PRESSED) { e ->
            if( e.code == ESCAPE ) {
                inputKeyword.requestFocus()
            }
        }

        descGridPane.allChildren.let{ children ->
            children.filterIsInstance<TextInputControl>().forEach {
                it.addEventFilter(KEY_PRESSED) { e ->
                    if( e.code == ESCAPE ) {
                        lastFocused = it
                        tableMain.requestFocus()
                    }
                }
                it.textProperty().onChange { buttonSave.isDisable = false }
            }
            children.filterIsInstance<CheckBox>().forEach {
                it.selectedProperty().addListener { _, _, _ -> buttonSave.isDisable = false }
            }
            children.filterIsInstance<TextArea>().forEach {
                it.addEventFilter(KEY_PRESSED) { e ->
                    if( e.code != TAB || e.isShiftDown || e.isControlDown ) return@addEventFilter
                    e.consume()
                    (e.source as Node).fireEvent(getKeyEventTab(e))
                }
            }
        }

        // 상세내역 변경시 버튼 컨트롤
        val listener: (observable: ObservableValue<*>, oldValue: Any?, newValue: Any?) -> Unit =
            { _,_,_ -> buttonSave.isDisable = false }
        descGridPane.children.filterIsInstance<TextInputControl>().forEach {
            it.textProperty().addListener(listener)
        }
        descIcon.imageProperty().addListener(listener)

    }

    private fun initSearchFilter() {

        val searchFilter = {
            val hasKeyword = inputKeyword.text.isNotBlank()
            val hasGroup   = inputGroup.text.isNotBlank()
            when {
                !hasGroup && !hasKeyword -> links.predicate = {true}
                !hasGroup &&  hasKeyword -> links.predicate = { keywordMatcher.isMatch(it.wordsAll) }
                 hasGroup && !hasKeyword -> links.predicate = { groupMatcher.isMatch(it.wordsGroup) }
                 hasGroup &&  hasKeyword -> links.predicate = { keywordMatcher.isMatch(it.wordsKeyword) && groupMatcher.isMatch(it.wordsGroup) }
            }
            printSearchResult()
        }

        var lastModified: LocalDateTime? = null

        timer(period = 100) {
            if( lastModified != null && now().between(lastModified!!).toMillis() < 300 ) {
                lastModified = null
                runLater {
                    listOf(inputKeyword,inputGroup).forEach { it.isDisable = true }
                    searchFilter()
                    listOf(inputKeyword,inputGroup).forEach { it.isDisable = false }
                }
            }
        }

        inputKeyword.textProperty().onChange{
            keywordMatcher.setKeyword(it)
            lastModified = now()
        }
        inputGroup.textProperty().onChange{
            groupMatcher.setKeyword(it)
            lastModified = now()
        }

    }

    private fun changeIcon() {
        if( detail == null ) return
        linkService.openIconPicker()?.let { changeIcon(it) }
    }

    private fun fnDraggable(e: DragEvent) {
        if (e.dragboard.hasFiles()) {
            e.acceptTransferModes(TransferMode.COPY)
        } else {
            e.consume()
        }
    }

    private fun changeIcon(file: File) {
        detail?.setIcon(file)?.let { icon ->
            descIcon.image = icon
            buttonSave.isDisable = false
        }
    }

    private fun getKeyEventTab(event: KeyEvent) = KeyEvent(
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
    )

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
        buttonNew.isDisable    = false
        buttonDelete.isDisable = false
        buttonCopy.isDisable   = false
        buttonSave.isDisable   = true
    }

    fun drawDetailForAdd(link: Link?) {
        drawDetail(link)
        buttonNew.isDisable    = false
        buttonDelete.isDisable = true
        buttonCopy.isDisable   = true
        buttonSave.isDisable   = false
        descGroupName.requestFocus()
    }

    fun deleteLink(link: Link?) {

        if( link == null ) return

        val summary = if( ! link.group.isNullOrEmpty() ) "[${link.group}] ${link.title}" else "${link.title}"

        if( ! Dialog.confirm("msg.confirm.001".message().format(summary)) ) return

        val prev = tableMain.focused

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
        runLater {
            labelCmd.text = command ?: ""
        }
    }

    fun printStatus(status: String? = null) {
        labelStatus.text = status ?: ""
    }

    fun printSearchResult() {
        printStatus("msg.info.005".message().format(links.size, links.items.size) )
    }

}