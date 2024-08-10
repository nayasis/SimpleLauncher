@file:Suppress("MemberVisibilityCanBePrivate","JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package com.github.nayasis.simplelauncher.view

import com.github.nayasis.kotlin.basica.core.extension.ifNull
import com.github.nayasis.kotlin.basica.core.localdate.between
import com.github.nayasis.kotlin.basica.core.localdate.toString
import com.github.nayasis.kotlin.basica.core.string.message
import com.github.nayasis.kotlin.basica.etc.error
import com.github.nayasis.kotlin.javafx.control.basic.allChildren
import com.github.nayasis.kotlin.javafx.control.basic.hmargin
import com.github.nayasis.kotlin.javafx.control.basic.repack
import com.github.nayasis.kotlin.javafx.control.tableview.column.cellValue
import com.github.nayasis.kotlin.javafx.control.tableview.column.cellValueByDefault
import com.github.nayasis.kotlin.javafx.control.tableview.column.setAlign
import com.github.nayasis.kotlin.javafx.control.tableview.focus
import com.github.nayasis.kotlin.javafx.control.tableview.focusBy
import com.github.nayasis.kotlin.javafx.control.tableview.focused
import com.github.nayasis.kotlin.javafx.control.tableview.scrollBy
import com.github.nayasis.kotlin.javafx.control.tableview.select
import com.github.nayasis.kotlin.javafx.control.tableview.selectBy
import com.github.nayasis.kotlin.javafx.control.tableview.visibleRows
import com.github.nayasis.kotlin.javafx.geometry.Insets
import com.github.nayasis.kotlin.javafx.misc.Desktop
import com.github.nayasis.kotlin.javafx.misc.runSync
import com.github.nayasis.kotlin.javafx.misc.set
import com.github.nayasis.kotlin.javafx.misc.toImage
import com.github.nayasis.kotlin.javafx.preloader.BasePreloader
import com.github.nayasis.kotlin.javafx.property.StageProperty
import com.github.nayasis.kotlin.javafx.stage.Dialog
import com.github.nayasis.kotlin.javafx.stage.Localizator
import com.github.nayasis.kotlin.javafx.stage.loadDefaultIcon
import com.github.nayasis.kotlin.javafx.stage.progress.ProgressDialog
import com.github.nayasis.simplelauncher.common.Context
import com.github.nayasis.simplelauncher.common.ICON_NEW
import com.github.nayasis.simplelauncher.model.Link
import com.github.nayasis.simplelauncher.service.LinkExecutor
import com.github.nayasis.simplelauncher.service.LinkService
import com.github.nayasis.simplelauncher.service.TextMatcher
import impl.org.controlsfx.autocompletion.AutoCompletionTextFieldBinding
import impl.org.controlsfx.autocompletion.SuggestionProvider
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
import javafx.scene.input.MouseEvent
import javafx.scene.input.TransferMode
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import mu.KotlinLogging
import tornadofx.*
import java.io.File
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import kotlin.concurrent.timer
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

private const val CLASS_AUTO_COMPLETER = "auto-completer"
private const val CLASS_ON_DRAG        = "table-row-on-drag"
private const val DEFAULT_LINK_EDITOR_WIDTH = 400.0

class Main: View("application.title".message()), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.JavaFx

    private val linkService: LinkService by di()
    private val linkExecutor: LinkExecutor by di()

    override val root: AnchorPane by fxml("/view/main/main.fxml")

    val tableMain: TableView<Link> by fxid()
    val colGroup: TableColumn<Link,String> by fxid()
    val colTitle: TableColumn<Link,Link> by fxid()
    val colLastUsedDt: TableColumn<Link,LocalDateTime?> by fxid()
    val colExecCount: TableColumn<Link,Int> by fxid()

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
    val descSeqExecution: CheckBox by fxid()
    val descTitle: TextField by fxid()
    val descHashtag: TextField by fxid()
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

    val keywordMatcher = TextMatcher()
    val groupMatcher = TextMatcher()

    private var lastFocused: Node? = null

    private val favicon       = resources.image("/image/icon/favicon.png")
    private val faviconPinned = resources.image("/image/icon/favicon-pinned.png")

    init {
        Localizator(root)
        initEvent()
        initTable()
    }

    override fun onBeforeShow() {
        logger.debug { ">> start before show" }
        currentStage?.loadDefaultIcon()
        Context.config.stageMain?.let {
            try {
                it.excludeKlass.add(Button::class)
                it.bind(currentStage)
                menubarTop.repack()
            } catch (e: Throwable) {
                logger.error(e)
            }
        }

        initSearchFilter(Context.config.lastFocusedRow)

        runSync {
            val total = linkService.countAll()
            linkService.loadAll { i, link ->
                BasePreloader.notifyProgress(i+1, total, link.title)
            }
        }

        printSearchResult()
        BasePreloader.close()

        logger.debug { ">> end before show" }

    }

    override fun onUndock() {
        Context.config.run {
            lastFocusedRow = tableMain.focused.row
            stageMain = StageProperty(currentStage!!)
            save()
        }
        exitProcess(0)
    }

    private fun initTable() {

        colGroup.cellValue(Link::group)
        colTitle.cellValueByDefault().cellFormat {
            graphic = hbox {
                imageview {
                    image = it.icon
                    hmargin = Insets(0,0,0,2)
                }
                label {
                    text = it.title ?: ""
                    hmargin = Insets(0,0,0,5)
                }
                alignment = Pos.CENTER_LEFT
            }
        }

        colTitle.setComparator { o1, o2 -> o1.title.ifNull{""}.compareTo(o2.title.ifNull{""}) }

        colLastUsedDt.cellValue(Link::executedAt).cellFormat {
            graphic = label {
                text = it?.toString("YYYY-MM-DD HH:MI:SS")
            }
            alignment = Pos.CENTER
        }
        colExecCount.cellValue(Link::executeCount).setAlign(Pos.CENTER_RIGHT)

        linkService.links.bindTo(tableMain)

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

        tableMain.setOnKeyPressed { e ->
            when(e.code) {
                ENTER -> tableMain.selectedItem?.let { linkExecutor.run(it) }
                ESCAPE -> inputKeyword.requestFocus()
                DELETE -> tableMain.selectedItem?.let{ deleteLink(it) }
                TAB -> {
                    if( ! e.isShiftDown ) {
                        e.consume()
                        (if(lastFocused == null || lastFocused == tableMain) descGroupName else lastFocused)!!.requestFocus()
                    }
                }
                C -> if(e.isControlDown) {
                    tableMain.selectedItem?.let {
                        e.consume()
                        linkService.copyFolder(it)
                    }
                }

                else -> {}
            }
        }

        tableMain.setRowFactory {
            TableRow<Link>().apply {
                val row = this
                setOnDragOver { event ->
                    if( row.isEmpty ) return@setOnDragOver
                    if( hasFile(event) ) {
                        if( ! row.styleClass.contains(CLASS_ON_DRAG) ) {
                            row.styleClass.add(CLASS_ON_DRAG)
                        }
                    }
                }
                setOnDragExited {
                    row.styleClass.remove(CLASS_ON_DRAG)
                }
                setOnDragDropped { event ->
                    event.run {
                        dragboard.let {
                            if( it.hasFiles() ) {
                                tableMain.focusBy(row.item)
                                linkExecutor.run(row.item,it.files)
                            }
                        }
                        isDropCompleted = true
                        consume()
                    }
                }
            }
        }

        currentStage?.requestFocus()

        logger.debug { ">> done initialize" }

    }

    private fun initEvent() {

        // global shortcut
        root.setOnKeyPressed { e ->
            if( e.isControlDown ) {
                when(e.code) {
                    S -> buttonSave.let { if(!it.isDisable) it.fire() }
                    D -> buttonCopy.let { if(!it.isDisable) it.fire() }
                    N -> {
                        e.consume()
                        if( e.isShiftDown) {
                            buttonAddFile.fireEvent(MOUSE_CLICK)
                        } else {
                            buttonNew.let { if(!it.isDisable) it.fire() }
                        }
                    }
                    O -> buttonOpenFolder.let { if(!it.isDisable) it.fire() }
                    C -> {
                        e.consume()
                        if( e.isShiftDown ) {
                            buttonCopyFolder.let { if(!it.isDisable) it.fire() }
                        }
                    }
                    I -> if(!e.isShiftDown) changeIcon()
                    else -> {}
                }
            } else if (e.isShiftDown ) {
                when(e.code) {
                    DELETE -> buttonDelete.let { if(!it.isDisable) it.fire() }
                    else -> {}
                }
            }
        }

        menuImportData.setOnAction {
            linkService.openImportPicker()?.let { file ->
                runSync {
                    launch {
                        linkService.importData(file)
                        linkService.loadAll()
                        Dialog.alert( "msg.success.import".message().format(file) )
                    }
                }
            }
        }

        menuExportData.setOnAction {
            linkService.openExportPicker()?.let { file ->
                linkService.exportData(file)
                Dialog.alert( "msg.success.export".message().format(file) )
            }
        }

        menuDeleteAll.setOnAction {
            if(Dialog.confirm("msg.confirm.delete.all".message())) {
                linkService.deleteAll()
                clearDetail()
            }
        }

        menuViewDesc.selectedProperty().addListener { _, _, show ->
            (tableMain.parent as HBox).children.also {
                if(show && descGridPane !in it) {
                    it.add(descGridPane)
                    currentStage?.let { stage -> stage.width += if(descGridPane.width <= 0) DEFAULT_LINK_EDITOR_WIDTH else descGridPane.width }
                } else {
                    if(descGridPane.width > 0 ) {
                        currentStage?.let { stage -> stage.width -= descGridPane.width }
                    }
                    it.remove(descGridPane)
                }
            }
        }

        menuViewMenuBar.selectedProperty().addListener { _, _, show ->
            menubarTop.let {
                it.isVisible = show
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

        menuAlwaysOnTop.selectedProperty().addListener { _, _, alwaysOnTop ->
            primaryStage.isAlwaysOnTop = alwaysOnTop
            when {
                alwaysOnTop -> setStageIcon(faviconPinned)
                else        -> setStageIcon(favicon)
            }
        }

        menuHelp.setOnAction{
            Desktop.browse("stage.help.url".message())
        }

        buttonSave.setOnAction { saveDetail() }
        buttonDelete.setOnAction { detail?.let{ deleteLink(it) } }
        buttonCopy.setOnAction { copyDetail() }
        buttonNew.setOnAction { createDetail() }
        buttonOpenFolder.setOnAction { tableMain.selectedItem?.let { linkService.openFolder(it) } }
        buttonCopyFolder.setOnAction { tableMain.selectedItem?.let { linkService.copyFolder(it) } }

        descIcon.setOnMouseClicked { e ->
            if( e.button == MouseButton.PRIMARY && e.clickCount > 1 )
                changeIcon()
        }
        descIcon.setOnDragOver { hasFile(it) }
        descIcon.setOnDragDropped { e ->
            e.dragboard.files.firstOrNull()?.let {
                changeIcon(it)
            }
        }

        descIcon.tooltip("btn.change.icon.tooltip".message())
        buttonAddFile.tooltip("btn.addfile.tooltip".message())

        descExecPath.setOnDragOver { hasFile(it) }
        descExecPath.setOnDragDropped { e ->
            e.dragboard.files.firstOrNull()?.let {
                detail?.setPath(it)
                descExecPath.text = detail?.path
                changeIcon(it)
            }
        }

        buttonAddFile.setOnDragOver { hasFile(it) }
        buttonAddFile.setOnDragDropped { e ->
            e.dragboard.files.forEach { file ->
                drawDetailForAdd(Link(file))
                currentStage?.requestFocus()
            }
        }
        buttonAddFile.setOnMouseClicked { e ->
            if( e.button == MouseButton.PRIMARY ) {
                linkService.openExecutorPicker()?.let { path ->
                    drawDetailForAdd(Link(path.toFile()))
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
                it.addEventFilter(KEY_PRESSED) { e ->
                    if( e.code == ESCAPE ) {
                        lastFocused = it
                        tableMain.requestFocus()
                    }
                }
                it.selectedProperty().addListener { _, _, _ -> buttonSave.isDisable = false }
            }
            children.filterIsInstance<TextArea>().forEach {
                it.addEventFilter(KEY_PRESSED) { e ->
                    if( e.code == ESCAPE ) {
                        lastFocused = it
                        tableMain.requestFocus()
                    } else if( e.code == TAB && ! e.isShiftDown && ! e.isControlDown ) {
                        e.consume()
                        (e.source as Node).fireEvent(toTabPressEvent(e))
                    }
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

    private fun initSearchFilter(focused: Int?) {
        keywordMatcher.setKeyword(inputKeyword.text)
        groupMatcher.setKeyword(inputGroup.text)
        setSearchFilter()
        focused?.let { tableMain.focus(it) }
        setSearchEvent()
    }

    private fun setSearchFilter() {
        val hasKeyword = inputKeyword.text.isNotBlank()
        val hasGroup   = inputGroup.text.isNotBlank()
        linkService.links.predicate = {
            val inKeyword = ! hasKeyword || keywordMatcher.isMatch(it.keywordTitle)
            val inGroup   = ! hasGroup   || groupMatcher.isMatch(it.keywordGroup)
            inKeyword && inGroup
        }
        printSearchResult()
    }

    private fun setSearchEvent() {

        var lastModified: LocalDateTime? = null

        timer(period = 100) {
            if( lastModified != null && now().between(lastModified!!).toMillis() < 300 ) {
                lastModified = null
                runLater {
                    listOf(inputKeyword,inputGroup).forEach { it.isDisable = true }
                    setSearchFilter()
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

        applyAutoCompletion(inputKeyword, Context.config.historyKeyword)

    }

    private fun applyAutoCompletion(textField: TextField, suggestion: HistorySet<String>) {
        var autoCompleter: AutoCompletionText? = null
        textField.addEventFilter(KEY_PRESSED) { e ->
            when {
                e.code == ESCAPE -> {
                    textField.removeClass(CLASS_AUTO_COMPLETER)
                    autoCompleter?.dispose()
                    autoCompleter = null
                }
                e.isAltDown -> {
                    when (e.code) {
                        DOWN -> if( autoCompleter == null ) {
                            textField.addClass(CLASS_AUTO_COMPLETER)
                            autoCompleter = AutoCompletionText(textField, suggestion)
                            autoCompleter?.setOnAutoCompleted {
                                textField.removeClass(CLASS_AUTO_COMPLETER)
                                autoCompleter?.dispose()
                                autoCompleter = null
                                setSearchFilter()
                            }
                            autoCompleter?.show()
                        }
                        LEFT  -> suggestion.prev()?.let { textField.text = it }
                        RIGHT -> suggestion.next()?.let { textField.text = it }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun changeIcon() {
        if( detail == null ) return
        linkService.openIconPicker()?.let { changeIcon(it.toFile()) }
    }

    private fun hasFile(e: DragEvent): Boolean {
        return if (e.dragboard.hasFiles()) {
            e.acceptTransferModes(TransferMode.COPY)
            true
        } else {
            false
        }
    }

    private fun changeIcon(file: File) {
        detail?.setIcon(file)?.let { icon ->
            descIcon.image = icon
            buttonSave.isDisable = false
        }
    }

    private fun toTabPressEvent(event: KeyEvent) = KeyEvent(
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

    private fun clearDetail() {
        // reset description
        descTitle.text               = null
        descHashtag.text                 = null
        descShowConsole.isSelected   = false
        descSeqExecution.isSelected  = false
        descGroupName.text           = null
        descDescription.text         = null
        descExecPath.text            = null
        descArg.text                 = null
        descCmdPrefix.text           = null
        descCmdPrev.text             = null
        descCmdNext.text             = null
        descIcon.image               = null
        // reset button status
        buttonNew.isDisable    = false
        buttonDelete.isDisable = false
        buttonCopy.isDisable   = false
        buttonSave.isDisable   = false
    }

    fun drawDetail(link: Link?) {
        if( link == null || detail?.id == link.id ) return
        detail = link
        with(detail!!) {
            descTitle.text               = title
            descHashtag.text                 = hashtag
            descShowConsole.isSelected   = showConsole
            descSeqExecution.isSelected  = executeEach
            descGroupName.text           = group
            descDescription.text         = description
            descExecPath.text            = path
            descArg.text                 = argument
            descCmdPrefix.text           = commandPrefix
            descCmdPrev.text             = commandPrev
            descCmdNext.text             = commandNext
            descIcon.image               = icon
        }
        buttonNew.isDisable    = false
        buttonDelete.isDisable = false
        buttonCopy.isDisable   = false
        buttonSave.isDisable   = true
    }

    fun drawDetailForAdd(link: Link?) {
        // expand editor
        menuViewDesc.isSelected = true

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

        if( ! Dialog.confirm("msg.confirm.delete".message().format(summary)) ) return

        val prev = tableMain.focused

        linkService.delete(link)

        clearDetail()
        tableMain.select(prev.row)

        printSearchResult()

    }

    fun saveDetail() {
        detail?.let {

            it.title         = descTitle.text?.trim()
            it.hashtag       = descHashtag.text?.trim()
            it.showConsole   = descShowConsole.isSelected
            it.executeEach   = descSeqExecution.isSelected
            it.group         = descGroupName.text?.trim()
            it.description   = descDescription.text?.trim()
            it.path          = descExecPath.text?.trim()
            it.argument      = descArg.text?.trim()
            it.commandPrefix = descCmdPrefix.text?.trim()
            it.commandPrev   = descCmdPrev.text
            it.commandNext   = descCmdNext.text
            it.icon          = descIcon.image

            linkService.save(it.indexing())

            runLater {
                tableMain.selectBy(it)
                tableMain.scrollBy(it)
                printSearchResult()
                printStatus("msg.alert.save.link".message())
            }

            buttonDelete.isDisable = false
            buttonCopy.isDisable = false
            buttonSave.isDisable = true
        }
    }

    fun copyDetail() {
        if( detail == null || detail!!.id <= 0 ) return
        drawDetail( detail!!.clone() )
        descTitle.requestFocus()
        printStatus("msg.alert.copy.link".message())
        buttonDelete.isDisable = true
        buttonCopy.isDisable = true
        buttonSave.isDisable = false
    }

    fun createDetail() {
        drawDetail(Link( icon = ICON_NEW.toImage() ))
        descGroupName.requestFocus()
        printStatus("msg.alert.create.link".message())
        buttonDelete.isDisable = true
        buttonCopy.isDisable = true
        buttonSave.isDisable = false
    }

    fun printCommand(command: String? = null) = runLater {
        labelCmd.text = command ?: ""
    }

    fun printStatus(status: String? = null) {
        labelStatus.text = status ?: ""
    }

    fun printSearchResult() = printStatus("msg.status.filter".message().format(
        linkService.links.size,
        linkService.links.items.size
    ))

}

private val MOUSE_CLICK = MouseEvent(
    MouseEvent.MOUSE_CLICKED,
    0.0,
    0.0,
    0.0,
    0.0,
    MouseButton.PRIMARY,
    1,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    null
)

class AutoCompletionText(
    val textField: TextField,
    val suggestion: HistorySet<String>
): AutoCompletionTextFieldBinding<String>(textField, SuggestionProvider.create(suggestion.toList())) {
    fun show() {
        if(suggestion.isEmpty() || textField.text.isBlank() ) return
        super.setUserInput(textField.text)
        super.showPopup()
    }
}