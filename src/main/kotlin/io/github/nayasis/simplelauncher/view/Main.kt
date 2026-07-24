@file:Suppress("MemberVisibilityCanBePrivate","JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package io.github.nayasis.simplelauncher.view

import impl.org.controlsfx.autocompletion.AutoCompletionTextFieldBinding
import impl.org.controlsfx.autocompletion.SuggestionProvider
import io.github.nayasis.kotlin.basica.core.extension.ifNull
import io.github.nayasis.kotlin.basica.core.localdate.toString
import io.github.nayasis.kotlin.basica.core.string.message
import io.github.nayasis.kotlin.basica.etc.error
import io.github.nayasis.kotlin.javafx.control.basic.allChildren
import io.github.nayasis.kotlin.javafx.control.basic.hmargin
import io.github.nayasis.kotlin.javafx.control.basic.repack
import io.github.nayasis.kotlin.javafx.control.tableview.*
import io.github.nayasis.kotlin.javafx.control.tableview.column.cellValue
import io.github.nayasis.kotlin.javafx.control.tableview.column.cellValueByDefault
import io.github.nayasis.kotlin.javafx.geometry.Insets
import io.github.nayasis.kotlin.javafx.misc.Desktop
import io.github.nayasis.kotlin.javafx.misc.runAwait
import io.github.nayasis.kotlin.javafx.misc.set
import io.github.nayasis.kotlin.javafx.preloader.DefaultPreloader
import io.github.nayasis.kotlin.javafx.property.StageProperty
import io.github.nayasis.kotlin.javafx.stage.Dialog
import io.github.nayasis.kotlin.javafx.stage.Localizator
import io.github.nayasis.kotlin.javafx.stage.loadDefaultIcon
import io.github.nayasis.kotlin.javafx.stage.WindowHeaderHelper
import io.github.nayasis.simplelauncher.common.Context
import io.github.nayasis.simplelauncher.common.ICON_NEW
import io.github.nayasis.simplelauncher.model.formatTokens
import io.github.nayasis.simplelauncher.model.Link
import io.github.nayasis.simplelauncher.service.LinkExecutor
import io.github.nayasis.simplelauncher.service.LinkService
import io.github.nayasis.simplelauncher.service.ShortcutAction
import io.github.nayasis.simplelauncher.service.ShortcutSettings
import io.github.nayasis.simplelauncher.service.TextMatcher
import io.github.nayasis.simplelauncher.service.matchesSearchTokens
import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.animation.PauseTransition
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.input.*
import javafx.scene.input.KeyCode.*
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.stage.Stage
import javafx.stage.WindowEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import tornadofx.*
import java.io.File
import java.time.LocalDateTime
import kotlin.coroutines.CoroutineContext
import javafx.util.Duration
import javafx.util.StringConverter
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

private const val CLASS_AUTO_COMPLETER = "auto-completer"
private const val CLASS_ON_DRAG        = "table-row-on-drag"
private const val DEFAULT_LINK_EDITOR_WIDTH = 400.0
private const val MIN_LINK_EDITOR_WIDTH = 300.0
private const val TABLE_ICON_SIZE = 18.0
private val TABLE_ITEM_SHORTCUT_ACTIONS = listOf(
    ShortcutAction.RUN_SELECTED_LINK,
    ShortcutAction.COPY_FOLDER_FROM_TABLE,
    ShortcutAction.DELETE_SELECTED_LINK,
)

internal fun createTableItemContextMenu(
    shortcutSettings: () -> ShortcutSettings,
    onAction: (ShortcutAction) -> Unit,
): ContextMenu {
    val actionItems = TABLE_ITEM_SHORTCUT_ACTIONS.associateWith { action ->
        MenuItem(action.messageKey.message()).apply {
            userData = action
            setOnAction { onAction(action) }
        }
    }
    fun refreshAccelerators() {
        val shortcuts = shortcutSettings()
        actionItems.forEach { (action, item) -> item.accelerator = shortcuts[action] }
    }
    refreshAccelerators()
    return ContextMenu(
        actionItems.getValue(ShortcutAction.RUN_SELECTED_LINK),
        actionItems.getValue(ShortcutAction.COPY_FOLDER_FROM_TABLE),
        SeparatorMenuItem(),
        actionItems.getValue(ShortcutAction.DELETE_SELECTED_LINK),
    ).apply {
        setOnShowing { refreshAccelerators() }
    }
}

class Main: View("application.title".message()), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.JavaFx

    private val linkService: LinkService by di()
    private val linkExecutor: LinkExecutor by di()

    override val root: AnchorPane by fxml("/view/main/main.fxml")

    val titleBar: HBox by fxid()

    val contentPane: SplitPane by fxid()
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
    val menuShortcutSettings: MenuItem by fxid()

    val inputKeyword: SearchTokenField by fxid()
    val inputGroup: SearchTokenField by fxid()

    val buttonNew: Button by fxid()
    val buttonSave: Button by fxid()
    val buttonDelete: Button by fxid()
    val buttonOpenFolder: Button by fxid()
    val buttonCopyFolder: Button by fxid()
    val buttonCopy: Button by fxid()
    val buttonAddFile: ImageView by fxid()

    val descGridPane: GridPane by fxid()
    val descGroupName: TokenField by fxid()
    val descShowConsole: CheckBox by fxid()
    val descSeqExecution: CheckBox by fxid()
    val descTitle: TextField by fxid()
    val descHashtag: TokenField by fxid()
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
    private var descEditorWidth = Context.config.descEditorWidth?.takeIf { it > 0 } ?: DEFAULT_LINK_EDITOR_WIDTH
    private var ignoreDescEditorWidthChange = false
    private var childWindowLifecycleBound = false
    private var closeRequestBound = false
    private val searchSubmitTracker = SearchSubmitTracker()
    private var shortcuts = ShortcutSettings.load(Context.config.shortcuts)

    private val favicon       = resources.image("/image/icon/favicon.png")
    private val faviconPinned = resources.image("/image/icon/favicon-pinned.png")

    init {
        Localizator(root)
        WindowHeaderHelper(titleBar)
        initEvent()
        initTable()
        applyShortcutSettings()
    }

    override fun onBeforeShow() {
        currentStage?.loadDefaultIcon()
        bindChildWindowLifecycle()
        bindCloseRequest()

        // set minimum window size
        currentStage?.let { stage ->
            stage.minWidth  = 460.0
            stage.minHeight = 150.0
        }

        Context.config.stageMain?.let {
            try {
                it.excludeKlass.add(Button::class)
                it.bind(currentStage)
                menubarTop.repack()
            } catch (e: Throwable) {
                logger.error(e)
            }
        }

        runAwait {
            val total = linkService.countAll()
            linkService.loadAll { i, link ->
                DefaultPreloader.notifyProgress(i+1, total, link.title)
            }
        }

        initSearchFilter()

        runLater {
            linkService.links.firstOrNull { it.id == Context.config.lastFocusedLinkId }?.let {
                tableMain.selectBy(it)
                tableMain.scrollBy(it)
            } ?: tableMain.selectFirst()
        }

        DefaultPreloader.close()

    }

    private fun bindChildWindowLifecycle() {
        if(childWindowLifecycleBound) return
        currentStage?.let(::bindChildWindowLifecycle) ?: root.sceneProperty().addListener { _, _, scene ->
            scene?.windowProperty()?.addListener { _, _, window ->
                (window as? Stage)?.let(::bindChildWindowLifecycle)
            }
        }
    }

    private fun bindChildWindowLifecycle(stage: Stage) {
        if(childWindowLifecycleBound) return
        stage.addEventHandler(WindowEvent.WINDOW_HIDDEN) {
            linkExecutor.hideChildWindows()
        }
        stage.addEventHandler(WindowEvent.WINDOW_SHOWN) {
            linkExecutor.restoreChildWindows()
        }
        stage.iconifiedProperty().addListener { _, _, iconified ->
            when(iconified) {
                true -> linkExecutor.hideChildWindows()
                else -> linkExecutor.restoreChildWindows()
            }
        }
        stage.showingProperty().addListener { _, _, showing ->
            when(showing) {
                true -> linkExecutor.restoreChildWindows()
                else -> linkExecutor.hideChildWindows()
            }
        }
        childWindowLifecycleBound = true
    }

    private fun bindCloseRequest() {
        if(closeRequestBound) return
        currentStage?.setOnCloseRequest { event ->
            if(!confirmClose()) {
                event.consume()
            } else {
                saveAppState()
            }
        }
        closeRequestBound = true
    }

    private fun confirmClose(): Boolean {
        val hasUnsavedChanges = hasUnsavedChanges()
        val hasRunningWork = linkExecutor.hasRunningWork()
        val messageKey = when {
            hasUnsavedChanges && hasRunningWork -> "msg.confirm.exit.pending.all"
            hasUnsavedChanges -> "msg.confirm.exit.pending.unsaved"
            hasRunningWork -> "msg.confirm.exit.pending.running"
            else -> return true
        }
        return Dialog.confirm(messageKey.message())
    }

    private fun hasUnsavedChanges(): Boolean {
        val link = detail ?: return false
        if(link.id <= 0) {
            return !buttonSave.isDisable
        }
        return isDetailModified(link)
    }

    private fun isDetailModified(link: Link): Boolean {
        return normalizedTrimmed(descTitle.text) != normalizedTrimmed(link.title)
            || descGroupName.hasPendingInput()
            || descGroupName.currentTokens() != link.group
            || descShowConsole.isSelected != link.showConsole
            || descSeqExecution.isSelected != link.executeEach
            || normalizedTrimmed(descDescription.text) != normalizedTrimmed(link.description)
            || normalizedTrimmed(descExecPath.text) != normalizedTrimmed(link.path)
            || normalizedTrimmed(descArg.text) != normalizedTrimmed(link.argument)
            || normalizedTrimmed(descCmdPrefix.text) != normalizedTrimmed(link.commandPrefix)
            || normalizedText(descCmdPrev.text) != normalizedText(link.commandPrev)
            || normalizedText(descCmdNext.text) != normalizedText(link.commandNext)
            || descHashtag.hasPendingInput()
            || descHashtag.currentTokens() != link.hashtag
            || descIcon.image != link.iconImage
    }

    private fun normalizedTrimmed(value: String?): String? {
        return value?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun normalizedText(value: String?): String? {
        return value?.takeIf { it.isNotEmpty() }
    }

    override fun onUndock() {
        saveAppState()
        exitProcess(0)
    }

    private fun saveAppState() {
        Context.config.run {
            updateDescEditorWidth()
            lastFocusedLinkId = tableMain.selectedItem?.id
            stageMain = StageProperty(currentStage!!)
            descEditorWidth = this@Main.descEditorWidth
            save()
        }
    }

    private fun initTable() {

        colGroup.setCellValueFactory { SimpleStringProperty(formatTokens(it.value.group)) }
        colTitle.cellValueByDefault().cellFormat {
            graphic = hbox {
                imageview {
                    image = it.iconImage
                    fitWidth = TABLE_ICON_SIZE
                    fitHeight = TABLE_ICON_SIZE
                    isPreserveRatio = true
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
            text = it?.toString("YYYY-MM-DD HH:MI:SS")
            alignment = Pos.CENTER
        }
        colExecCount.cellValue(Link::executeCount).cellFormat {
            text = it.toString()
            alignment = Pos.CENTER_RIGHT
        }


        linkService.links.bindTo(tableMain)

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

        tableMain.onSelectionChange { link ->
            drawDetail(link)
        }

        tableMain.setOnKeyPressed { e ->
            when {
                shortcuts.matches(ShortcutAction.RUN_SELECTED_LINK, e) -> {
                    tableMain.selectedItem?.let { performTableItemAction(ShortcutAction.RUN_SELECTED_LINK, it) }
                }
                shortcuts.matches(ShortcutAction.FOCUS_SEARCH, e) -> {
                    inputKeyword.focusInput()
                }
                shortcuts.matches(ShortcutAction.DELETE_SELECTED_LINK, e) -> {
                    tableMain.selectedItem?.let {
                        e.consume()
                        performTableItemAction(ShortcutAction.DELETE_SELECTED_LINK, it)
                    }
                }
                shortcuts.matches(ShortcutAction.MOVE_FOCUS_TO_DETAIL, e) && !e.isShiftDown -> {
                    e.consume()
                    if(lastFocused == null || lastFocused == tableMain) {
                        descGroupName.focusInput()
                    } else {
                        lastFocused!!.requestFocus()
                    }
                }
                shortcuts.matches(ShortcutAction.COPY_FOLDER_FROM_TABLE, e) -> {
                    tableMain.selectedItem?.let {
                        e.consume()
                        performTableItemAction(ShortcutAction.COPY_FOLDER_FROM_TABLE, it)
                    }
                }
            }
        }

        tableMain.setRowFactory {
            TableRow<Link>().apply {
                val row = this
                val rowMenu = createTableItemContextMenu({ shortcuts }) { action ->
                    row.item?.let { performTableItemAction(action, it) }
                }
                itemProperty().addListener { _, _, item ->
                    contextMenu = if(item == null) null else rowMenu
                }
                setOnMousePressed { event ->
                    if(event.button == MouseButton.SECONDARY && !row.isEmpty) {
                        tableMain.selectionModel.select(row.item)
                    }
                }
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

    }

    private fun performTableItemAction(action: ShortcutAction, link: Link) {
        when(action) {
            ShortcutAction.RUN_SELECTED_LINK -> linkExecutor.run(link)
            ShortcutAction.COPY_FOLDER_FROM_TABLE -> linkService.copyFolder(link)
            ShortcutAction.DELETE_SELECTED_LINK -> deleteLink(link)
            else -> Unit
        }
    }

    private fun initEvent() {

        descGridPane.maxWidth = Double.MAX_VALUE
        descGridPane.widthProperty().addListener { _, _, _ -> updateDescEditorWidth() }

        root.setOnKeyPressed { e ->
            when {
                shortcuts.matches(ShortcutAction.SAVE_DETAIL, e) -> {
                    buttonSave.let { if(!it.isDisable) it.fire() }
                }
                shortcuts.matches(ShortcutAction.COPY_DETAIL, e) -> {
                    buttonCopy.let { if(!it.isDisable) it.fire() }
                }
                shortcuts.matches(ShortcutAction.ADD_FILE, e) -> {
                    e.consume()
                    buttonAddFile.fireEvent(MOUSE_CLICK)
                }
                shortcuts.matches(ShortcutAction.CREATE_DETAIL, e) -> {
                    e.consume()
                    buttonNew.let { if(!it.isDisable) it.fire() }
                }
                shortcuts.matches(ShortcutAction.OPEN_FOLDER, e) -> {
                    buttonOpenFolder.let { if(!it.isDisable) it.fire() }
                }
                shortcuts.matches(ShortcutAction.COPY_FOLDER, e) -> {
                    e.consume()
                    buttonCopyFolder.let { if(!it.isDisable) it.fire() }
                }
                shortcuts.matches(ShortcutAction.CHANGE_ICON, e) -> {
                    changeIcon()
                }
                shortcuts.matches(ShortcutAction.DELETE_DETAIL, e) -> {
                    buttonDelete.let { if(!it.isDisable) it.fire() }
                }
            }
        }

        menuImportData.setOnAction {
            linkService.openImportPicker()?.let { file ->
                runAwait {
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

        menuShortcutSettings.setOnAction {
            ShortcutEditor().showDialog(currentStage, shortcuts)?.let { updated ->
                shortcuts = updated
                Context.config.shortcuts = shortcuts.toConfigMap()
                Context.config.save()
                applyShortcutSettings()
            }
        }

        menuViewDesc.selectedProperty().addListener { _, _, show ->
            when {
                show -> showDescEditor()
                else -> hideDescEditor()
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
                    inputGroup.focusInput()
                } else {
                    it.remove(group)
                    inputGroup.clearTokens()
                    submitSearch()
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

        inputKeyword.onPlainEnter = {
            handleSearchEnter(saveKeywordHistory = true)
        }
        inputKeyword.onEscape = {
            tableMain.requestFocus()
        }

        inputGroup.onEscape = {
            inputKeyword.focusInput()
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
        descHashtag.addEventFilter(KEY_PRESSED) { e ->
            if( e.code == ESCAPE ) {
                lastFocused = descHashtag
                tableMain.requestFocus()
            }
        }
        descHashtag.onTokenChanged = { buttonSave.isDisable = false }
        descGroupName.addEventFilter(KEY_PRESSED) { e ->
            if( e.code == ESCAPE ) {
                lastFocused = descGroupName
                tableMain.requestFocus()
            }
        }
        descGroupName.onTokenChanged = { buttonSave.isDisable = false }
        descIcon.imageProperty().addListener(listener)

    }

    private fun applyShortcutSettings() {
        menuImportData.accelerator = shortcuts[ShortcutAction.IMPORT_DATA]
        menuExportData.accelerator = shortcuts[ShortcutAction.EXPORT_DATA]
        menuViewDesc.accelerator = shortcuts[ShortcutAction.TOGGLE_DESCRIPTION]
        menuViewMenuBar.accelerator = shortcuts[ShortcutAction.TOGGLE_MENU_BAR]
        menuShowInputGroup.accelerator = shortcuts[ShortcutAction.TOGGLE_GROUP_FILTER]
        menuAlwaysOnTop.accelerator = shortcuts[ShortcutAction.TOGGLE_ALWAYS_ON_TOP]
        menuHelp.accelerator = shortcuts[ShortcutAction.SHOW_ABOUT]
        updateShortcutTooltips()
    }

    private fun updateShortcutTooltips() {
        buttonSave.tooltip(shortcuts.displayText(ShortcutAction.SAVE_DETAIL))
        buttonDelete.tooltip(shortcuts.displayText(ShortcutAction.DELETE_DETAIL))
        buttonCopy.tooltip(shortcuts.displayText(ShortcutAction.COPY_DETAIL))
        buttonNew.tooltip(shortcuts.displayText(ShortcutAction.CREATE_DETAIL))
        buttonOpenFolder.tooltip(shortcuts.displayText(ShortcutAction.OPEN_FOLDER))
        buttonCopyFolder.tooltip(shortcuts.displayText(ShortcutAction.COPY_FOLDER))
        descIcon.tooltip("${"shortcut.action.changeIcon".message()} (${shortcuts.displayText(ShortcutAction.CHANGE_ICON)})")
        buttonAddFile.tooltip("${"shortcut.action.addFile".message()} (${shortcuts.displayText(ShortcutAction.ADD_FILE)})")
    }

    private fun updateDescEditorWidth() {
        if(ignoreDescEditorWidthChange) {
            return
        }
        if(descGridPane !in contentPane.items) {
            return
        }
        if(descGridPane.width > 0) {
            descEditorWidth = descGridPane.width
        }
    }

    private fun showDescEditor() {
        if(descGridPane in contentPane.items) return
        val width = descEditorWidth
        val targetPaneWidth = contentPane.width + width
        ignoreDescEditorWidthChange = true
        constrainDescEditorWidth(width)
        contentPane.items.add(descGridPane)
        currentStage?.let { stage -> stage.width += width }
        restoreDescEditorWidthWhenReady(width, targetPaneWidth)
    }

    private fun hideDescEditor() {
        if(descGridPane !in contentPane.items) return
        val width = descGridPane.width.takeIf { it > 0 } ?: descEditorWidth
        descEditorWidth = width
        ignoreDescEditorWidthChange = true
        if(contentPane.width > 0 && descGridPane.width > 0) {
            currentStage?.let { stage -> stage.width -= width }
        }
        contentPane.items.remove(descGridPane)
        runLater {
            descEditorWidth = width
            ignoreDescEditorWidthChange = false
        }
    }

    private fun restoreDescEditorWidth(editorWidth: Double = descEditorWidth) {
        if(descGridPane !in contentPane.items) {
            return
        }
        val width = contentPane.width
        if(width <= editorWidth) {
            return
        }
        val divider = ((width - splitPaneDividerWidth() - editorWidth) / width).coerceIn(0.1, 0.9)
        contentPane.setDividerPositions(divider)
        descEditorWidth = editorWidth
    }

    private fun restoreDescEditorWidthWhenReady(editorWidth: Double, targetPaneWidth: Double) {
        var applied = false
        var listener: ChangeListener<Number>? = null
        fun applyIfReady(force: Boolean) {
            if(applied) return
            val ready = isDescEditorLayoutReady(targetPaneWidth)
            if(!ready && !force) return
            applied = true
            listener?.let { contentPane.widthProperty().removeListener(it) }
            restoreDescEditorWidth(editorWidth)
            PauseTransition(Duration.millis(80.0)).apply {
                setOnFinished {
                    releaseDescEditorWidthConstraint(editorWidth)
                    descEditorWidth = descGridPane.width.takeIf { it > 0 } ?: editorWidth
                    ignoreDescEditorWidthChange = false
                }
                play()
            }
        }
        listener = ChangeListener { _, _, _ ->
            runLater { applyIfReady(false) }
        }
        contentPane.widthProperty().addListener(listener)
        runLater { applyIfReady(false) }
        PauseTransition(Duration.millis(250.0)).apply {
            setOnFinished { applyIfReady(true) }
            play()
        }
    }

    private fun isDescEditorLayoutReady(targetPaneWidth: Double): Boolean {
        return contentPane.width >= targetPaneWidth - 1
            && tableMain.width > 0
            && descGridPane.width > 0
            && splitPaneDividerWidth() > 0
    }

    private fun splitPaneDividerWidth(): Double {
        val dividerNodeWidth = contentPane.lookupAll(".split-pane-divider")
            .sumOf { it.boundsInParent.width }
            .takeIf { it > 0 }
        if(dividerNodeWidth != null) return dividerNodeWidth

        val itemWidth = contentPane.items.sumOf { it.boundsInParent.width }
        return (contentPane.width - itemWidth).coerceAtLeast(0.0)
    }

    private fun constrainDescEditorWidth(width: Double) {
        descGridPane.minWidth = width
        descGridPane.prefWidth = width
        descGridPane.maxWidth = width
    }

    private fun releaseDescEditorWidthConstraint(width: Double) {
        descGridPane.minWidth = MIN_LINK_EDITOR_WIDTH
        descGridPane.prefWidth = width
        descGridPane.maxWidth = Double.MAX_VALUE
    }

    private fun initSearchFilter() {
        restoreSearchState()
        keywordMatcher.setKeyword(inputKeyword.text)
        groupMatcher.setKeyword(inputGroup.text)
        setSearchFilter()
        setSearchEvent()
    }

    private fun setSearchFilter() {
        val hasKeyword = inputKeyword.text.isNotBlank()
        val hasGroup   = inputGroup.text.isNotBlank()
        linkService.links.predicate = {
            val keywordHaystack = listOf(it.title) + it.hashtag
            val inKeyword = when {
                inputKeyword.hasTokens() -> matchesSearchTokens(keywordHaystack, inputKeyword.tokens())
                hasKeyword -> keywordMatcher.isMatch(keywordHaystack)
                else -> true
            }
            val inGroup = when {
                inputGroup.hasTokens() -> matchesSearchTokens(it.group, inputGroup.tokens())
                hasGroup -> groupMatcher.isMatch(it.group)
                else -> true
            }
            inKeyword && inGroup
        }
        printSearchResult()
    }

    private fun setSearchEvent() {

        inputGroup.onPlainEnter = {
            handleSearchEnter()
        }

        applySearchHistory(inputKeyword) { compactSearchHistory(Context.config.historySearch) }
        applyAutoCompletion(inputGroup.input, { groupTokenSuggestions(inputGroup.currentTermValues()) })
        applyLiveAutoCompletion(descGroupName.input, { allGroupTokenSuggestions(descGroupName.currentTokens()) }) {
            descGroupName.commitInput()
        }
        applyAutoCompletion(descHashtag.input, { hashtagSuggestions() }) {
            descHashtag.commitInput()
        }

    }

    private fun restoreSearchState() {
        Context.config.lastSearchState
            ?.let { inputKeyword.setSearchState(it) }
            ?: inputKeyword.setSearchText(Context.config.lastSearchKeyword)
    }

    private fun submitSearch(saveKeywordHistory: Boolean = false) {
        keywordMatcher.setKeyword(inputKeyword.text)
        groupMatcher.setKeyword(inputGroup.text)
        setSearchFilter()
        if(saveKeywordHistory) {
            saveSubmittedKeyword()
        }
    }

    private fun handleSearchEnter(saveKeywordHistory: Boolean = false) {
        val snapshot = searchSubmitSnapshot()
        when(searchSubmitTracker.nextAction(snapshot)) {
            SearchSubmitAction.SUBMIT -> {
                submitSearch(saveKeywordHistory)
                if(snapshot.isBlank() && tableMain.visibleRows in 1..10) {
                    runFirstSearchResult()
                }
            }
            SearchSubmitAction.RUN_FIRST_RESULT -> runFirstSearchResult()
        }
    }

    private fun searchSubmitSnapshot(): SearchSubmitSnapshot {
        return SearchSubmitSnapshot(
            keyword = inputKeyword.searchText(),
            group = inputGroup.searchText(),
        )
    }

    private fun runFirstSearchResult() {
        val link = tableMain.items.firstOrNull() ?: return
        tableMain.focus(0)
        tableMain.selectionModel.select(link)
        linkExecutor.run(link)
    }

    private fun saveSubmittedKeyword() {
        val state = inputKeyword.searchState().takeIf { !it.isBlank() }
        Context.config.lastSearchState = state
        Context.config.lastSearchKeyword = state?.displayText()
        state?.let { replaceSearchHistory(it) }
        Context.config.save()
    }

    private fun replaceSearchHistory(state: SearchFieldState) {
        val displayText = state.displayText()
        Context.config.historySearch.toList()
            .filter { it.displayText() == displayText }
            .forEach { Context.config.historySearch.remove(it) }
        Context.config.historySearch.add(state)
    }

    private fun compactSearchHistory(history: HistorySet<SearchFieldState>): HistorySet<SearchFieldState> {
        val statesByText = LinkedHashMap<String, SearchFieldState>()
        history.toList().forEach { state ->
            val displayText = state.displayText()
            if(displayText.isNotBlank()) {
                val existing = statesByText[displayText]
                if(existing == null || state.tokens.isNotEmpty() || existing.tokens.isEmpty()) {
                    statesByText.remove(displayText)
                    statesByText[displayText] = state
                }
            }
        }
        return HistorySet<SearchFieldState>(statesByText.size.coerceAtLeast(1)).apply {
            statesByText.values.forEach { add(it) }
        }
    }

    private fun applySearchHistory(
        field: SearchTokenField,
        suggestions: () -> HistorySet<SearchFieldState>,
    ) {
        var autoCompleter: SearchHistoryAutoCompletionText? = null
        var suggestion: HistorySet<SearchFieldState>? = null

        fun setSearch(state: SearchFieldState?) {
            state ?: return
            field.setSearchState(state)
        }

        field.input.addEventFilter(KEY_PRESSED) { e ->
            when {
                e.code == ESCAPE -> {
                    field.input.removeClass(CLASS_AUTO_COMPLETER)
                    autoCompleter?.dispose()
                    autoCompleter = null
                    suggestion = null
                }
                e.isAltDown -> {
                    when (e.code) {
                        DOWN -> if(autoCompleter == null) {
                            suggestion = suggestions()
                            field.input.addClass(CLASS_AUTO_COMPLETER)
                            autoCompleter = SearchHistoryAutoCompletionText(field.input, suggestion!!)
                            autoCompleter?.setOnAutoCompleted { event ->
                                field.input.removeClass(CLASS_AUTO_COMPLETER)
                                autoCompleter?.dispose()
                                autoCompleter = null
                                suggestion = null
                                field.setSearchState(event.completion)
                            }
                            autoCompleter?.show()
                        }
                        LEFT -> {
                            setSearch((suggestion ?: suggestions().also { suggestion = it }).prev())
                            e.consume()
                        }
                        RIGHT -> {
                            setSearch((suggestion ?: suggestions().also { suggestion = it }).next())
                            e.consume()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun applyAutoCompletion(
        textField: TextField,
        suggestions: () -> HistorySet<String>,
        onAutoCompleted: (() -> Unit)? = null,
    ) {
        var autoCompleter: AutoCompletionText? = null
        var suggestion: HistorySet<String>? = null
        textField.addEventFilter(KEY_PRESSED) { e ->
            when {
                e.code == ESCAPE -> {
                    textField.removeClass(CLASS_AUTO_COMPLETER)
                    autoCompleter?.dispose()
                    autoCompleter = null
                    suggestion = null
                }
                e.isAltDown -> {
                    when (e.code) {
                        DOWN -> if( autoCompleter == null ) {
                            suggestion = suggestions()
                            textField.addClass(CLASS_AUTO_COMPLETER)
                            autoCompleter = AutoCompletionText(textField, suggestion!!)
                            autoCompleter?.setOnAutoCompleted {
                                textField.removeClass(CLASS_AUTO_COMPLETER)
                                autoCompleter?.dispose()
                                autoCompleter = null
                                suggestion = null
                                onAutoCompleted?.invoke()
                            }
                            autoCompleter?.show()
                        }
                        LEFT  -> (suggestion ?: suggestions().also { suggestion = it }).prev()?.let { textField.text = it }
                        RIGHT -> (suggestion ?: suggestions().also { suggestion = it }).next()?.let { textField.text = it }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun applyLiveAutoCompletion(
        textField: TextField,
        suggestions: () -> HistorySet<String>,
        onAutoCompleted: (() -> Unit)? = null,
    ) {
        val autoCompleter = AutoCompletionTextFieldBinding<String>(textField) { request ->
            val userText = request.userText?.trim() ?: ""
            if(request.isCancelled || userText.isBlank()) {
                emptyList()
            } else {
                suggestions().toList().filter { it.contains(userText, ignoreCase = true) }
            }
        }
        autoCompleter.setDelay(0)
        autoCompleter.setOnAutoCompleted {
            onAutoCompleted?.invoke()
        }
        textField.properties["liveAutoCompleter"] = autoCompleter
    }

    private fun hashtagSuggestions(): HistorySet<String> {
        return HistorySet<String>(512).apply {
            linkService.hashtagSuggestions().forEach { add(it) }
        }
    }

    private fun allGroupTokenSuggestions(excludedTokens: Iterable<String>): HistorySet<String> {
        return HistorySet<String>(512).apply {
            linkService.groupTokenSuggestions(excludedTokens).forEach { add(it) }
        }
    }

    private fun groupTokenSuggestions(selectedTokens: Iterable<String>): HistorySet<String> {
        return HistorySet<String>(512).apply {
            linkService.groupTokenSuggestions(selectedTokens, selectedTokens).forEach { add(it) }
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
        descHashtag.clearTokens()
        descShowConsole.isSelected   = false
        descSeqExecution.isSelected  = false
        descGroupName.clearTokens()
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
            descHashtag.setTokens(hashtag)
            descShowConsole.isSelected   = showConsole
            descSeqExecution.isSelected  = executeEach
            descGroupName.setTokens(group)
            descDescription.text         = description
            descExecPath.text            = path
            descArg.text                 = argument
            descCmdPrefix.text           = commandPrefix
            descCmdPrev.text             = commandPrev
            descCmdNext.text             = commandNext
            descIcon.image               = iconImage
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
        descGroupName.focusInput()
    }

    fun deleteLink(link: Link?) {

        if( link == null ) return

        val group = formatTokens(link.group)
        val summary = if( group.isNotEmpty() ) "[$group] ${link.title}" else "${link.title}"

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
            it.hashtag       = descHashtag.getTokens()
            it.showConsole   = descShowConsole.isSelected
            it.executeEach   = descSeqExecution.isSelected
            it.group         = descGroupName.getTokens()
            it.description   = descDescription.text?.trim()
            it.path          = descExecPath.text?.trim()
            it.argument      = descArg.text?.trim()
            it.commandPrefix = descCmdPrefix.text?.trim()
            it.commandPrev   = descCmdPrev.text
            it.commandNext   = descCmdNext.text
            it.iconImage     = descIcon.image

            linkService.save(it)

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
        drawDetail(Link(icon = ICON_NEW))
        descGroupName.focusInput()
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

class SearchHistoryAutoCompletionText(
    val textField: TextField,
    val suggestion: HistorySet<SearchFieldState>
): AutoCompletionTextFieldBinding<SearchFieldState>(
    textField,
    SuggestionProvider.create({ it.displayText() }, suggestion.toList()),
    object: StringConverter<SearchFieldState>() {
        override fun toString(value: SearchFieldState?): String {
            return value?.displayText() ?: ""
        }

        override fun fromString(value: String?): SearchFieldState {
            return SearchFieldState(text = value?.trim())
        }
    },
) {
    fun show() {
        if(suggestion.isEmpty() || textField.text.isBlank() ) return
        super.setUserInput(textField.text)
        super.showPopup()
    }
}
