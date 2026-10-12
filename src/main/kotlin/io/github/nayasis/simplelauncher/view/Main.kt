@file:Suppress("MemberVisibilityCanBePrivate")

package io.github.nayasis.simplelauncher.view

import com.formdev.flatlaf.FlatClientProperties
import io.github.nayasis.kotlin.basica.core.localdate.toString
import io.github.nayasis.kotlin.basica.core.string.message
import io.github.nayasis.kotlin.basica.core.string.toPath
import io.github.nayasis.kotlin.basica.etc.error
import io.github.nayasis.kotlin.swing.app.SwingView
import io.github.nayasis.kotlin.swing.collection.HistorySet
import io.github.nayasis.kotlin.swing.control.AutoCompletion
import io.github.nayasis.kotlin.swing.control.addTextChanged
import io.github.nayasis.kotlin.swing.control.table.ListTableModel
import io.github.nayasis.kotlin.swing.control.table.ScrollMode
import io.github.nayasis.kotlin.swing.control.table.bind
import io.github.nayasis.kotlin.swing.control.table.clipTextInsteadOfEllipsis
import io.github.nayasis.kotlin.swing.control.table.findColumnBy
import io.github.nayasis.kotlin.swing.control.table.focusBy
import io.github.nayasis.kotlin.swing.control.table.installColumnVisibilityMenu
import io.github.nayasis.kotlin.swing.control.table.scrollBy
import io.github.nayasis.kotlin.swing.control.table.select
import io.github.nayasis.kotlin.swing.control.table.selectBy
import io.github.nayasis.kotlin.swing.control.table.visibleRows
import io.github.nayasis.kotlin.swing.control.token.TokenField
import io.github.nayasis.kotlin.swing.control.token.TokenInputPane
import io.github.nayasis.kotlin.swing.misc.Desktop
import io.github.nayasis.kotlin.swing.misc.displayText
import io.github.nayasis.kotlin.swing.misc.launchUi
import io.github.nayasis.kotlin.swing.misc.onFileDrop
import io.github.nayasis.kotlin.swing.misc.onLookAndFeelChange
import io.github.nayasis.kotlin.swing.misc.removeAlpha
import io.github.nayasis.kotlin.swing.misc.runLater
import io.github.nayasis.kotlin.swing.misc.set
import io.github.nayasis.kotlin.swing.window.Dialog
import io.github.nayasis.kotlin.swing.window.Windows
import io.github.nayasis.kotlin.swing.window.addCloseRequest
import io.github.nayasis.kotlin.swing.window.isMaximized
import io.github.nayasis.kotlin.swing.window.loadDefaultIcon
import io.github.nayasis.kotlin.swing.window.runWhenShown
import io.github.nayasis.kotlin.swing.window.watchMaximized
import io.github.nayasis.kotlin.swing.property.WindowProperty
import io.github.nayasis.simplelauncher.common.Context
import io.github.nayasis.simplelauncher.common.ICON_NEW
import io.github.nayasis.simplelauncher.model.Link
import io.github.nayasis.simplelauncher.model.formatTokens
import io.github.nayasis.simplelauncher.model.normalizeHashtags
import io.github.nayasis.simplelauncher.service.LinkExecutor
import io.github.nayasis.simplelauncher.service.LinkService
import io.github.nayasis.simplelauncher.service.ShortcutAction
import io.github.nayasis.simplelauncher.service.ShortcutSettings
import io.github.nayasis.simplelauncher.service.TextMatcher
import io.github.nayasis.simplelauncher.service.matchesSearchTokens
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Image
import java.awt.Insets
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.ItemEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import java.io.File
import java.time.LocalDateTime
import java.util.WeakHashMap
import javax.imageio.ImageIO
import javax.swing.BorderFactory
import javax.swing.DropMode
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JCheckBoxMenuItem
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JRadioButtonMenuItem
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.MenuSelectionManager
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.text.JTextComponent

private val logger = KotlinLogging.logger {}

private const val DEFAULT_LINK_EDITOR_WIDTH = 400
private const val MIN_LINK_EDITOR_WIDTH = 300
private const val MIN_TABLE_WIDTH = 150
private const val TABLE_ICON_SIZE = 18
private const val TABLE_ROW_HEIGHT = 24
private const val DETAIL_ICON_SIZE = 23
private const val ADD_FILE_ICON_SIZE = 26
private val TABLE_ITEM_SHORTCUT_ACTIONS = listOf(
    ShortcutAction.RUN_SELECTED_LINK,
    ShortcutAction.COPY_FOLDER_FROM_TABLE,
    ShortcutAction.OPEN_FOLDER_FROM_TABLE,
    ShortcutAction.DELETE_SELECTED_LINK,
)

internal const val CONTEXT_MENU_SHOW_DETAIL_ID = "context-menu-show-detail"
internal const val CONTEXT_MENU_ACTION_KEY = "shortcutAction"

internal class MainShortcutHandlers(
    val onSaveDetail: () -> Unit = {},
    val onCopyDetail: () -> Unit = {},
    val onAddFile: () -> Unit = {},
    val onCreateDetail: () -> Unit = {},
    val onOpenFolder: () -> Unit = {},
    val onCopyFolder: () -> Unit = {},
    val onChangeIcon: () -> Unit = {},
    val onDeleteDetail: () -> Unit = {},
    val onImportData: () -> Unit = {},
    val onExportData: () -> Unit = {},
    val onOpenShortcutSettings: () -> Unit = {},
    val onToggleDescription: () -> Unit = {},
    val onToggleGroupFilter: () -> Unit = {},
    val onToggleAlwaysOnTop: () -> Unit = {},
    val onShowAbout: () -> Unit = {},
    val onRunSelectedLink: () -> Unit = {},
    val onFocusSearch: () -> Unit = {},
    val onDeleteSelectedLink: () -> Unit = {},
    val onMoveFocusToDetail: () -> Unit = {},
    val onCopyFolderFromTable: () -> Unit = {},
    val onOpenFolderFromTable: () -> Unit = {},
)

internal fun handleMainShortcutEvent(
    shortcuts: ShortcutSettings,
    event: KeyEvent,
    tableShortcutContext: Boolean,
    handlers: MainShortcutHandlers,
): Boolean {
    when {
        shortcuts.matches(ShortcutAction.IMPORT_DATA, event) -> handlers.onImportData()
        shortcuts.matches(ShortcutAction.EXPORT_DATA, event) -> handlers.onExportData()
        shortcuts.matches(ShortcutAction.OPEN_SHORTCUT_SETTINGS, event) -> handlers.onOpenShortcutSettings()
        shortcuts.matches(ShortcutAction.TOGGLE_DESCRIPTION, event) -> handlers.onToggleDescription()
        shortcuts.matches(ShortcutAction.TOGGLE_GROUP_FILTER, event) -> handlers.onToggleGroupFilter()
        shortcuts.matches(ShortcutAction.TOGGLE_ALWAYS_ON_TOP, event) -> handlers.onToggleAlwaysOnTop()
        shortcuts.matches(ShortcutAction.SHOW_ABOUT, event) -> handlers.onShowAbout()
        tableShortcutContext && shortcuts.matches(ShortcutAction.RUN_SELECTED_LINK, event) -> handlers.onRunSelectedLink()
        tableShortcutContext && shortcuts.matches(ShortcutAction.FOCUS_SEARCH, event) -> handlers.onFocusSearch()
        tableShortcutContext && shortcuts.matches(ShortcutAction.DELETE_SELECTED_LINK, event) -> handlers.onDeleteSelectedLink()
        tableShortcutContext && shortcuts.matches(ShortcutAction.MOVE_FOCUS_TO_DETAIL, event) && !event.isShiftDown -> handlers.onMoveFocusToDetail()
        tableShortcutContext && shortcuts.matches(ShortcutAction.COPY_FOLDER_FROM_TABLE, event) -> handlers.onCopyFolderFromTable()
        tableShortcutContext && shortcuts.matches(ShortcutAction.OPEN_FOLDER_FROM_TABLE, event) -> handlers.onOpenFolderFromTable()
        shortcuts.matches(ShortcutAction.SAVE_DETAIL, event) -> handlers.onSaveDetail()
        shortcuts.matches(ShortcutAction.COPY_DETAIL, event) -> handlers.onCopyDetail()
        shortcuts.matches(ShortcutAction.ADD_FILE, event) -> handlers.onAddFile()
        shortcuts.matches(ShortcutAction.CREATE_DETAIL, event) -> handlers.onCreateDetail()
        shortcuts.matches(ShortcutAction.OPEN_FOLDER, event) -> handlers.onOpenFolder()
        shortcuts.matches(ShortcutAction.COPY_FOLDER, event) -> handlers.onCopyFolder()
        shortcuts.matches(ShortcutAction.CHANGE_ICON, event) -> handlers.onChangeIcon()
        shortcuts.matches(ShortcutAction.DELETE_DETAIL, event) -> handlers.onDeleteDetail()
        else -> return false
    }
    event.consume()
    return true
}

internal fun createTableItemContextMenu(
    shortcutSettings: () -> ShortcutSettings,
    onAction: (ShortcutAction) -> Unit,
    onShowDetail: () -> Unit,
    onCopyIcon: () -> Unit = {},
    onSaveIcon: () -> Unit = {},
): JPopupMenu {
    val actionItems = TABLE_ITEM_SHORTCUT_ACTIONS.associateWith { action ->
        JMenuItem(action.messageKey.message()).apply {
            putClientProperty(CONTEXT_MENU_ACTION_KEY, action)
            addActionListener { onAction(action) }
        }
    }
    val showDetailItem = JMenuItem("shortcut.action.showDetailEditor".message()).apply {
        name = CONTEXT_MENU_SHOW_DETAIL_ID
        addActionListener { onShowDetail() }
    }
    val copyIconItem = JMenuItem("contextmenu.icon.copy".message()).apply {
        addActionListener { onCopyIcon() }
    }
    val saveIconItem = JMenuItem("contextmenu.icon.save".message()).apply {
        addActionListener { onSaveIcon() }
    }
    fun refreshAccelerators() {
        val shortcuts = shortcutSettings()
        actionItems.forEach { (action, item) -> item.accelerator = shortcuts[action] }
        showDetailItem.accelerator = shortcuts[ShortcutAction.TOGGLE_DESCRIPTION]
    }
    refreshAccelerators()
    return JPopupMenu().apply {
        add(actionItems.getValue(ShortcutAction.RUN_SELECTED_LINK))
        add(showDetailItem)
        addSeparator()
        add(actionItems.getValue(ShortcutAction.COPY_FOLDER_FROM_TABLE))
        add(actionItems.getValue(ShortcutAction.OPEN_FOLDER_FROM_TABLE))
        addSeparator()
        add(copyIconItem)
        add(saveIconItem)
        addSeparator()
        add(actionItems.getValue(ShortcutAction.DELETE_SELECTED_LINK))
        addPopupMenuListener(object: PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) = refreshAccelerators()
            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {}
            override fun popupMenuCanceled(e: PopupMenuEvent) {}
        })
    }
}

/** [image] fitted into a [size] square, keeping its aspect ratio. */
private fun scaledIcon(image: Image, size: Int): ImageIcon {
    val width  = image.getWidth(null).coerceAtLeast(1)
    val height = image.getHeight(null).coerceAtLeast(1)
    val scale  = size.toDouble() / maxOf(width, height)
    val w = (width * scale).toInt().coerceAtLeast(1)
    val h = (height * scale).toInt().coerceAtLeast(1)
    return ImageIcon(image.getScaledInstance(w, h, Image.SCALE_SMOOTH))
}

private fun readResourceImage(path: String): BufferedImage? =
    runCatching { Main::class.java.getResource(path)?.let { ImageIO.read(it) } }.getOrNull()

class Main: SwingView("application.title".message()) {

    private val linkService: LinkService by lazy { Context.linkService }
    private val linkExecutor: LinkExecutor by lazy { Context.linkExecutor }

    // ─── menu ────────────────────────────────────────────────────────────────

    val menuImportData       = JMenuItem("menu.file.import".message())
    val menuExportData       = JMenuItem("menu.file.export".message())
    val menuDeleteAll        = JMenuItem("menu.file.clear".message())
    val menuShortcutSettings = JMenuItem("menu.settings.shortcut".message())
    // named components are saved and restored with the window state
    val menuViewDesc         = JCheckBoxMenuItem("menu.view.description".message(), true).apply { name = "menuViewDesc" }
    val menuShowInputGroup   = JCheckBoxMenuItem("menu.view.showInputGroup".message(), true).apply { name = "menuShowInputGroup" }
    val menuAlwaysOnTop      = JCheckBoxMenuItem("menu.view.alwaysOnTop".message(), false).apply { name = "menuAlwaysOnTop" }
    val menuHelp             = JMenuItem("menu.help.about".message())
    val menuTheme            = JMenu("menu.view.theme".message()).apply {
        name = "menuTheme"
        val group   = ButtonGroup()
        val current = AppTheme.of(Context.config.theme)
        AppTheme.entries.forEach { theme ->
            add(JRadioButtonMenuItem(theme.label, theme == current).apply {
                group.add(this)
                addActionListener { changeTheme(theme) }
            })
        }
    }

    val menubarTop = JMenuBar().apply {
        add(JMenu("menu.file".message()).apply {
            add(menuImportData)
            add(menuExportData)
            add(menuDeleteAll)
        })
        add(JMenu("menu.settings".message()).apply {
            add(menuShortcutSettings)
        })
        add(JMenu("menu.view".message()).apply {
            add(menuViewDesc)
            add(menuShowInputGroup)
            add(menuAlwaysOnTop)
            addSeparator()
            add(menuTheme)
        })
        add(JMenu("menu.help".message()).apply {
            add(menuHelp)
        })
    }

    // ─── search bar ──────────────────────────────────────────────────────────

    val inputKeyword = SearchTokenField().apply {
        minimumSize = Dimension(100, preferredSize.height)
    }

    val inputGroup = SearchTokenField().apply {
        preferredSize = Dimension(220, preferredSize.height)
        minimumSize = Dimension(140, preferredSize.height)
    }

    private val groupFilter = JPanel(BorderLayout(5, 0)).apply {
        isOpaque = false
        border = BorderFactory.createEmptyBorder(0, 10, 0, 0)
        add(JLabel("search.group".message()), BorderLayout.WEST)
        add(inputGroup, BorderLayout.CENTER)
    }

    val buttonAddFile = JLabel().apply {
        readResourceImage("/image/icon/add.png")?.let { icon = scaledIcon(it, ADD_FILE_ICON_SIZE) }
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    }

    private val searchBar = JPanel(GridBagLayout()).apply {
        border = BorderFactory.createEmptyBorder(2, 0, 2, 0)
        val c = GridBagConstraints().apply { gridy = 0; anchor = GridBagConstraints.WEST }
        add(JLabel("search.keyword".message()), c.apply { gridx = 0; insets = Insets(0, 2, 0, 0) })
        add(inputKeyword, c.apply { gridx = 1; weightx = 1.0; fill = GridBagConstraints.HORIZONTAL; insets = Insets(0, 5, 0, 0) })
        add(groupFilter, c.apply { gridx = 2; weightx = 0.0; fill = GridBagConstraints.NONE; insets = Insets(0, 0, 0, 0) })
        add(buttonAddFile, c.apply { gridx = 3; insets = Insets(0, 15, 0, 10) })
    }

    // ─── table ───────────────────────────────────────────────────────────────

    val tableModel = ListTableModel(linkService.links)
        .column<String>("table.groupName".message(), id = "colGroup", getter = { formatTokens(it.group) })
        .column<Link>("table.title".message(), id = "colTitle", comparator = compareBy { it.title.orEmpty() }, getter = { it })
        .column<LocalDateTime>("table.lastUsedDt".message(), id = "colLastUsedDt", getter = { it.executedAt })
        .column<Int>("table.execCount".message(), id = "colExecCount", getter = { it.executeCount })

    val tableMain = JTable().apply {
        name = "tableMain"
        bind(tableModel)
        rowHeight = TABLE_ROW_HEIGHT
        selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        fillsViewportHeight = true
        dropMode = DropMode.ON
        installColumnVisibilityMenu()
    }

    private val tableScroll = JScrollPane(tableMain).apply {
        minimumSize = Dimension(MIN_TABLE_WIDTH, 0)
    }

    // ─── detail editor ───────────────────────────────────────────────────────

    val descGroupName    = TokenField(8, ::normalizeHashtags)
    val descShowConsole  = JCheckBox("desc.showConsole".message())
    val descSeqExecution = JCheckBox("desc.execution.sequential".message())
    val descTitle        = detailTextField()
    val descHashtag      = TokenField(8, ::normalizeHashtags)
    val descDescription  = textArea()
    val descIcon         = JLabel().apply {
        preferredSize = Dimension(DETAIL_ICON_SIZE, DETAIL_ICON_SIZE)
        horizontalAlignment = SwingConstants.CENTER
    }
    val descExecPath     = detailTextField()
    val descArg          = detailTextField()
    val descCmdPrefix    = detailTextField()
    val descCmdPrev      = textArea()
    val descCmdNext      = textArea()

    val buttonNew        = button("btn.new")
    val buttonDelete     = button("btn.delete")
    val buttonCopy       = button("btn.copy")
    val buttonSave       = button("btn.save")
    val buttonOpenFolder = button("btn.open.folder")
    val buttonCopyFolder = button("btn.copy.folder")

    val descPanel = JPanel(GridBagLayout()).apply {
        minimumSize = Dimension(MIN_LINK_EDITOR_WIDTH, 0)
        border = BorderFactory.createEmptyBorder(0, 4, 0, 0)

        fun label(key: String, row: Int) = add(JLabel(key.message()), GridBagConstraints().apply {
            gridx = 0; gridy = row
            anchor = GridBagConstraints.NORTHEAST
            insets = Insets(6, 0, 3, 5)
        })
        fun field(component: Component, row: Int, weighty: Double = 0.0) = add(component, GridBagConstraints().apply {
            gridx = 1; gridy = row
            weightx = 1.0
            this.weighty = weighty
            fill = if (weighty > 0) GridBagConstraints.BOTH else GridBagConstraints.HORIZONTAL
            insets = Insets(2, 0, 2, 0)
        })
        fun row(vararg components: Component, stretch: Component? = null) = JPanel(GridBagLayout()).apply {
            isOpaque = false
            components.forEachIndexed { index, component ->
                add(component, GridBagConstraints().apply {
                    gridx = index
                    weightx = if (component === stretch) 1.0 else 0.0
                    fill = if (component === stretch) GridBagConstraints.HORIZONTAL else GridBagConstraints.NONE
                    anchor = GridBagConstraints.EAST
                    insets = Insets(0, if (index == 0) 0 else 5, 0, 0)
                })
            }
        }

        fun spacer() = JPanel().apply { isOpaque = false }

        spacer().let { field(row(it, buttonNew, buttonDelete, buttonCopy, buttonSave, stretch = it), 0) }
        label("desc.groupName", 1);   field(row(descGroupName, descShowConsole, descSeqExecution, stretch = descGroupName), 1)
        label("desc.title", 2);       field(descTitle, 2)
        label("desc.hashtag", 3);     field(descHashtag, 3)
        label("desc.description", 4); field(scroll(descDescription), 4, weighty = 2.0)
        label("desc.execPath", 5);    field(row(descIcon, descExecPath, stretch = descExecPath), 5)
        spacer().let { field(row(it, buttonCopyFolder, buttonOpenFolder, stretch = it), 6) }
        label("desc.arg", 7);         field(descArg, 7)
        label("desc.execCmdPrefix", 8); field(descCmdPrefix, 8)
        label("desc.cmdPrev", 9);     field(scroll(descCmdPrev), 9, weighty = 1.0)
        label("desc.cmdNext", 10);    field(scroll(descCmdNext), 10, weighty = 1.0)
    }

    private val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
        // extra width goes to the table, the editor keeps its width
        resizeWeight = 1.0
        isContinuousLayout = true
        border = BorderFactory.createEmptyBorder()
        leftComponent = tableScroll
        rightComponent = descPanel
    }

    private val contentHolder = JPanel(BorderLayout()).apply {
        add(splitPane, BorderLayout.CENTER)
    }

    // ─── status bar ──────────────────────────────────────────────────────────

    val labelStatus = JLabel(" ")
    val labelCmd    = JLabel(" ").apply { horizontalAlignment = SwingConstants.RIGHT }

    private val statusBar = JPanel(BorderLayout(10, 0)).apply {
        border = BorderFactory.createEmptyBorder(2, 0, 2, 0)
        add(labelStatus, BorderLayout.WEST)
        add(labelCmd, BorderLayout.CENTER)
    }

    override val root = JPanel(BorderLayout()).apply {
        border = BorderFactory.createEmptyBorder(0, 5, 0, 5)
        preferredSize = Dimension(1000, 659)
        add(searchBar, BorderLayout.NORTH)
        add(contentHolder, BorderLayout.CENTER)
        add(statusBar, BorderLayout.SOUTH)
    }

    // ─── state ───────────────────────────────────────────────────────────────

    var detail: Link? = null
    private var detailIcon: BufferedImage? = null

    val keywordMatcher = TextMatcher()
    val groupMatcher = TextMatcher()

    private var lastFocused: Component? = null
    private var descEditorWidth = Context.config.descEditorWidth?.toInt()?.takeIf { it > 0 } ?: DEFAULT_LINK_EDITOR_WIDTH
    // while the saved window state is applied, toggles must not resize the already restored window
    private var restoringState = false
    private val searchSubmitTracker = SearchSubmitTracker()
    private var searchJob: Job? = null
    private var shortcuts = ShortcutSettings.load(Context.config.shortcuts)

    private val faviconPinned = readResourceImage("/image/icon/favicon-pinned.png")

    private val tableMenu = createTableItemContextMenu(
        { shortcuts },
        { action -> selectedLink()?.let { performTableItemAction(action, it) } },
        { selectedLink()?.let { showDetailEditor(it) } },
        onCopyIcon = { selectedLink()?.let { copyIconToClipboard(it) } },
        onSaveIcon = { selectedLink()?.let { saveIconToFile(it) } },
    )

    init {
        initEvent()
        initTable()
        applyShortcutSettings()
        // the context menu and the half of the layout swapped out with the editor are outside the window
        onLookAndFeelChange {
            listOf(tableMenu, splitPane, tableScroll, descPanel)
                .filter { SwingUtilities.getWindowAncestor(it) == null }
                .forEach(SwingUtilities::updateComponentTreeUI)
        }
    }

    private fun changeTheme(theme: AppTheme) {
        if (theme == AppTheme.of(Context.config.theme)) return
        theme.apply()
        Context.config.theme = theme.name
        Context.config.save()
    }

    override fun onBeforeShow() {
        val frame = primaryFrame
        frame.jMenuBar = menubarTop
        // FlatLaf can only center the title or put it after the menus; draw it ourselves next to the window icon
        frame.rootPane.putClientProperty(FlatClientProperties.TITLE_BAR_SHOW_TITLE, false)
        menubarTop.add(JLabel(frame.title).apply {
            border = BorderFactory.createEmptyBorder(0, 2, 0, 12)
            putClientProperty(FlatClientProperties.COMPONENT_TITLE_BAR_CAPTION, true)
        }, 0)
        frame.minimumSize = Dimension(430, 150)
        frame.loadDefaultIcon()
        frame.watchMaximized()
        frame.defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        frame.addCloseRequest {
            if(confirmClose()) {
                saveAppState()
                frame.dispose()
            }
        }
        bindChildWindowLifecycle(frame)
        installShortcutDispatcher(frame)

        Context.config.windowMain?.let { property ->
            restoringState = true
            try {
                property.excludeKlass.add(JButton::class)
                property.bind(frame)
            } catch (e: Throwable) {
                logger.error(e)
            } finally {
                restoringState = false
            }
        }
        frame.runWhenShown { applyDescEditorWidth(descEditorWidth) }

        // filtering is asynchronous, so restore the selection after it is applied
        initSearchFilter {
            linkService.links.firstOrNull { it.id == Context.config.lastFocusedLinkId }?.let {
                tableMain.selectBy(it)
                tableMain.scrollBy(it)
            } ?: run { if (tableMain.rowCount > 0) tableMain.select(0) }
        }
    }

    private fun bindChildWindowLifecycle(frame: JFrame) {
        frame.addWindowListener(object: WindowAdapter() {
            override fun windowIconified(e: WindowEvent) = linkExecutor.hideChildWindows()
            override fun windowDeiconified(e: WindowEvent) = linkExecutor.restoreChildWindows()
        })
        frame.addComponentListener(object: ComponentAdapter() {
            override fun componentHidden(e: ComponentEvent) = linkExecutor.hideChildWindows()
            override fun componentShown(e: ComponentEvent) = linkExecutor.restoreChildWindows()
        })
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
            return buttonSave.isEnabled
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
            || detailIcon !== link.iconImage
    }

    private fun normalizedTrimmed(value: String?): String? {
        return value?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun normalizedText(value: String?): String? {
        return value?.takeIf { it.isNotEmpty() }
    }

    private fun saveAppState() {
        Context.config.run {
            if(isDescEditorShown) descPanel.width.takeIf { it > 0 }?.let { this@Main.descEditorWidth = it }
            lastFocusedLinkId = selectedLink()?.id
            windowMain = WindowProperty(primaryFrame)
            this.descEditorWidth = this@Main.descEditorWidth.toDouble()
            save()
        }
    }

    private fun selectedLink(): Link? =
        tableMain.selectedRow.takeIf { it in 0 until tableModel.rowCount }?.let { tableModel.getRowObject(it) }

    private fun initTable() {

        tableMain.findColumnBy("colGroup").apply { preferredWidth = 100; minWidth = 50 }
        tableMain.findColumnBy("colTitle").apply { preferredWidth = 253; minWidth = 80 }
        tableMain.findColumnBy("colLastUsedDt").apply {
            preferredWidth = 139
            minWidth = 80
            cellRenderer = object: DefaultTableCellRenderer() {
                init { horizontalAlignment = SwingConstants.CENTER }
                override fun setValue(value: Any?) {
                    text = (value as? LocalDateTime)?.toString("YYYY-MM-DD HH:MI:SS") ?: ""
                }
            }
        }
        tableMain.findColumnBy("colExecCount").apply { preferredWidth = 50; minWidth = 50; maxWidth = 200 }
        tableMain.findColumnBy("colTitle").cellRenderer = TitleRenderer()
        tableMain.clipTextInsteadOfEllipsis()

        tableMain.addMouseListener(object: MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if(SwingUtilities.isLeftMouseButton(e) && e.clickCount > 1) {
                    selectedLink()?.let { linkExecutor.run(it) }
                }
            }
            override fun mousePressed(e: MouseEvent) = showMenuIfTrigger(e)
            override fun mouseReleased(e: MouseEvent) = showMenuIfTrigger(e)
            private fun showMenuIfTrigger(e: MouseEvent) {
                if(!e.isPopupTrigger) return
                val row = tableMain.rowAtPoint(e.point).takeIf { it >= 0 } ?: return
                tableMain.select(row)
                tableMain.requestFocusInWindow()
                tableMenu.show(tableMain, e.x, e.y)
            }
        })

        tableMain.addFocusListener(object: FocusAdapter() {
            override fun focusGained(e: FocusEvent) {
                if(tableMain.selectedRow < 0 && tableMain.rowCount > 0) tableMain.select(0)
            }
        })

        tableMain.selectionModel.addListSelectionListener { e ->
            if(!e.valueIsAdjusting) selectedLink()?.let { drawDetail(it) }
        }

        tableMain.onFileDrop { files, support ->
            val row = (support.dropLocation as? JTable.DropLocation)?.row ?: -1
            if(row !in 0 until tableModel.rowCount) return@onFileDrop
            val link = tableModel.getRowObject(row)
            tableMain.focusBy(link)
            linkExecutor.run(link, files)
        }

    }

    private fun performTableItemAction(action: ShortcutAction, link: Link) {
        when(action) {
            ShortcutAction.RUN_SELECTED_LINK -> linkExecutor.run(link)
            ShortcutAction.COPY_FOLDER_FROM_TABLE -> linkService.copyFolder(link)
            ShortcutAction.OPEN_FOLDER_FROM_TABLE -> linkService.openFolder(link)
            ShortcutAction.DELETE_SELECTED_LINK -> deleteLink(link)
            else -> Unit
        }
    }

    private fun showDetailEditor(link: Link) {
        tableMain.focusBy(link)
        menuViewDesc.isSelected = !menuViewDesc.isSelected
        if (menuViewDesc.isSelected) {
            descGroupName.focusInput()
        }
    }

    private fun initEvent() {

        menuImportData.addActionListener {
            linkService.openImportPicker()?.let { file ->
                launchUi {
                    linkService.importData(file)
                    linkService.loadAll()
                    printSearchResult()
                    Dialog.alert( "msg.success.import".message().format(file) )
                }
            }
        }

        menuExportData.addActionListener {
            linkService.openExportPicker()?.let { file ->
                launchUi {
                    linkService.exportData(file)
                    Dialog.alert( "msg.success.export".message().format(file) )
                }
            }
        }

        menuDeleteAll.addActionListener {
            if(Dialog.confirm("msg.confirm.delete.all".message())) {
                launchUi {
                    linkService.deleteAll()
                    clearDetail()
                    printSearchResult()
                }
            }
        }

        menuShortcutSettings.addActionListener { openShortcutSettings() }

        menuViewDesc.addItemListener { e ->
            if(e.stateChange == ItemEvent.SELECTED) showDescEditor() else hideDescEditor()
        }

        menuShowInputGroup.addItemListener { e ->
            val show = e.stateChange == ItemEvent.SELECTED
            groupFilter.isVisible = show
            searchBar.revalidate()
            if(show) {
                if(!restoringState) inputGroup.focusInput()
            } else {
                inputGroup.clearTokens()
                submitSearch()
            }
        }

        menuAlwaysOnTop.addItemListener { e ->
            val alwaysOnTop = e.stateChange == ItemEvent.SELECTED
            primaryFrame.isAlwaysOnTop = alwaysOnTop
            primaryFrame.iconImages = when {
                alwaysOnTop && faviconPinned != null -> listOf(faviconPinned)
                else -> Windows.defaultIcons.icons
            }
        }

        menuHelp.addActionListener {
            Desktop.browse("stage.help.url".message())
        }

        buttonSave.addActionListener { saveDetail() }
        buttonDelete.addActionListener { detail?.let{ deleteLink(it) } }
        buttonCopy.addActionListener { copyDetail() }
        buttonNew.addActionListener { createDetail() }
        buttonOpenFolder.addActionListener { selectedLink()?.let { linkService.openFolder(it) } }
        buttonCopyFolder.addActionListener { selectedLink()?.let { linkService.copyFolder(it) } }

        descIcon.addMouseListener(object: MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if(SwingUtilities.isLeftMouseButton(e) && e.clickCount > 1) changeIcon()
            }
            override fun mousePressed(e: MouseEvent) = showMenuIfTrigger(e)
            override fun mouseReleased(e: MouseEvent) = showMenuIfTrigger(e)
            private fun showMenuIfTrigger(e: MouseEvent) {
                if(e.isPopupTrigger) showIconContextMenu(e)
            }
        })
        descIcon.onFileDrop { files, _ -> changeIcon(files.first()) }

        descExecPath.onFileDrop { files, _ ->
            val file = files.first()
            detail?.setPath(file)
            descExecPath.text = detail?.path
            changeIcon(file)
        }

        buttonAddFile.onFileDrop { files, _ ->
            files.forEach { file -> drawDetailForAdd(Link(file)) }
            primaryFrame.toFront()
        }
        buttonAddFile.addMouseListener(object: MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if(SwingUtilities.isLeftMouseButton(e)) addFileByPicker()
            }
        })

        labelCmd.addMouseListener(object: MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if(SwingUtilities.isLeftMouseButton(e) && e.clickCount > 1) {
                    Desktop.clipboard.set(labelCmd.text)
                }
            }
        })

        inputKeyword.onPlainEnter = {
            inputKeyword.commitPendingTextNextToChips()
            handleSearchEnter(saveKeywordHistory = true)
        }
        inputKeyword.onEscape = {
            tableMain.requestFocusInWindow()
        }

        inputGroup.onEscape = {
            inputKeyword.focusInput()
        }

        // Escape leaves the editor for the table; Tab moves on, even out of a text area
        val markEdited = { buttonSave.isEnabled = true }
        listOf<JTextComponent>(descTitle, descExecPath, descArg, descCmdPrefix, descDescription, descCmdPrev, descCmdNext).forEach { field ->
            field.leaveOnEscape(field)
            field.addTextChanged(markEdited)
        }
        listOf(descDescription, descCmdPrev, descCmdNext).forEach { area ->
            area.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null)
            area.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null)
        }
        listOf(descShowConsole, descSeqExecution).forEach { check ->
            check.leaveOnEscape(check)
            check.addItemListener { markEdited() }
        }
        listOf(descGroupName, descHashtag).forEach { tokens ->
            tokens.input.leaveOnEscape(tokens)
            tokens.onTokenChanged = markEdited
        }

        descIcon.toolTipText = "btn.change.icon.tooltip".message()
        buttonAddFile.toolTipText = "btn.addfile.tooltip".message()

    }

    /** on Escape, remember [target] for coming back with Tab, and move to the table. */
    private fun Component.leaveOnEscape(target: Component) {
        addKeyListener(object: KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if(e.isConsumed || e.keyCode != KeyEvent.VK_ESCAPE) return
                lastFocused = target
                tableMain.requestFocusInWindow()
            }
        })
    }

    private fun addFileByPicker() {
        linkService.openExecutorPicker()?.let { path ->
            drawDetailForAdd(Link(path.toFile()))
        }
    }

    private fun applyShortcutSettings() {
        menuImportData.accelerator = shortcuts[ShortcutAction.IMPORT_DATA]
        menuExportData.accelerator = shortcuts[ShortcutAction.EXPORT_DATA]
        menuShortcutSettings.accelerator = shortcuts[ShortcutAction.OPEN_SHORTCUT_SETTINGS]
        menuViewDesc.accelerator = shortcuts[ShortcutAction.TOGGLE_DESCRIPTION]
        menuShowInputGroup.accelerator = shortcuts[ShortcutAction.TOGGLE_GROUP_FILTER]
        menuAlwaysOnTop.accelerator = shortcuts[ShortcutAction.TOGGLE_ALWAYS_ON_TOP]
        menuHelp.accelerator = shortcuts[ShortcutAction.SHOW_ABOUT]
        updateShortcutTooltips()
    }

    /**
     * route the shortcuts of the main window, before any component sees the key.
     *
     * menu accelerators only show the bindings: this dispatcher consumes the keys first.
     */
    private fun installShortcutDispatcher(frame: JFrame) {
        val handlers = MainShortcutHandlers(
            onSaveDetail = { if(buttonSave.isEnabled) saveDetail() },
            onCopyDetail = { if(buttonCopy.isEnabled) copyDetail() },
            onAddFile = { addFileByPicker() },
            onCreateDetail = { if(buttonNew.isEnabled) createDetail() },
            onOpenFolder = { buttonOpenFolder.doClick() },
            onCopyFolder = { buttonCopyFolder.doClick() },
            onChangeIcon = { changeIcon() },
            onDeleteDetail = { if(buttonDelete.isEnabled) detail?.let { deleteLink(it) } },
            onImportData = { menuImportData.doClick() },
            onExportData = { menuExportData.doClick() },
            onOpenShortcutSettings = { openShortcutSettings() },
            onToggleDescription = { menuViewDesc.isSelected = !menuViewDesc.isSelected },
            onToggleGroupFilter = { menuShowInputGroup.isSelected = !menuShowInputGroup.isSelected },
            onToggleAlwaysOnTop = { menuAlwaysOnTop.isSelected = !menuAlwaysOnTop.isSelected },
            onShowAbout = { menuHelp.doClick() },
            onRunSelectedLink = { selectedLink()?.let { performTableItemAction(ShortcutAction.RUN_SELECTED_LINK, it) } },
            onFocusSearch = { inputKeyword.focusInput() },
            onDeleteSelectedLink = { selectedLink()?.let { performTableItemAction(ShortcutAction.DELETE_SELECTED_LINK, it) } },
            onMoveFocusToDetail = {
                when(val target = lastFocused) {
                    null, tableMain -> descGroupName.focusInput()
                    is TokenInputPane -> target.focusInput()
                    else -> target.requestFocusInWindow()
                }
            },
            onCopyFolderFromTable = { selectedLink()?.let { performTableItemAction(ShortcutAction.COPY_FOLDER_FROM_TABLE, it) } },
            onOpenFolderFromTable = { selectedLink()?.let { performTableItemAction(ShortcutAction.OPEN_FOLDER_FROM_TABLE, it) } },
        )
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(KeyEventDispatcher { event ->
            if(event.id != KeyEvent.KEY_PRESSED) return@KeyEventDispatcher false
            val source = event.component ?: return@KeyEventDispatcher false
            val window = source as? Window ?: SwingUtilities.getWindowAncestor(source)
            if(window !== frame) return@KeyEventDispatcher false
            // an open menu or popup navigates with the keys itself
            if(MenuSelectionManager.defaultManager().selectedPath.isNotEmpty()) return@KeyEventDispatcher false
            handleMainShortcutEvent(
                shortcuts = shortcuts,
                event = event,
                tableShortcutContext = SwingUtilities.isDescendingFrom(source, tableMain),
                handlers = handlers,
            )
        })
    }

    private fun openShortcutSettings() {
        ShortcutEditor().showDialog(primaryFrame, shortcuts)?.let { updated ->
            shortcuts = updated
            Context.config.shortcuts = shortcuts.toConfigMap()
            Context.config.save()
            applyShortcutSettings()
        }
    }

    private fun updateShortcutTooltips() {
        buttonSave.toolTipText = shortcuts.displayText(ShortcutAction.SAVE_DETAIL)
        buttonDelete.toolTipText = shortcuts.displayText(ShortcutAction.DELETE_DETAIL)
        buttonCopy.toolTipText = shortcuts.displayText(ShortcutAction.COPY_DETAIL)
        buttonNew.toolTipText = shortcuts.displayText(ShortcutAction.CREATE_DETAIL)
        buttonOpenFolder.toolTipText = shortcuts.displayText(ShortcutAction.OPEN_FOLDER)
        buttonCopyFolder.toolTipText = shortcuts.displayText(ShortcutAction.COPY_FOLDER)
        descIcon.toolTipText = "${"shortcut.action.changeIcon".message()} (${shortcuts.displayText(ShortcutAction.CHANGE_ICON)})"
        buttonAddFile.toolTipText = "${"shortcut.action.addFile".message()} (${shortcuts.displayText(ShortcutAction.ADD_FILE)})"
    }

    // ─── detail editor visibility ────────────────────────────────────────────

    private val isDescEditorShown: Boolean
        get() = splitPane.parent === contentHolder

    /** show the editor, widening the window by the editor so the table keeps its width. */
    private fun showDescEditor() {
        if(isDescEditorShown) return
        val width = descEditorWidth
        contentHolder.removeAll()
        splitPane.leftComponent = tableScroll
        splitPane.rightComponent = descPanel
        contentHolder.add(splitPane, BorderLayout.CENTER)
        resizeFrameWidth(width + splitPane.dividerSize)
        contentHolder.revalidate()
        contentHolder.repaint()
        applyDescEditorWidth(width)
    }

    /** hide the editor, narrowing the window by the editor. */
    private fun hideDescEditor() {
        if(!isDescEditorShown) return
        val width = descPanel.width.takeIf { it > 0 } ?: descEditorWidth
        descEditorWidth = width
        contentHolder.removeAll()
        contentHolder.add(tableScroll, BorderLayout.CENTER)
        resizeFrameWidth(-(width + splitPane.dividerSize))
        contentHolder.revalidate()
        contentHolder.repaint()
    }

    private fun resizeFrameWidth(delta: Int) {
        if(restoringState) return
        val frame = runCatching { primaryFrame }.getOrNull() ?: return
        if(!frame.isShowing || frame.isMaximized) return
        frame.setSize((frame.width + delta).coerceAtLeast(frame.minimumSize.width), frame.height)
        frame.validate()
    }

    private fun applyDescEditorWidth(editorWidth: Int) {
        if(!isDescEditorShown) return
        splitPane.validate()
        val total = splitPane.width
        if(total <= 0) {
            runLater { applyDescEditorWidth(editorWidth) }
            return
        }
        val location = (total - splitPane.dividerSize - editorWidth).coerceIn(MIN_TABLE_WIDTH, (total - splitPane.dividerSize - MIN_LINK_EDITOR_WIDTH).coerceAtLeast(MIN_TABLE_WIDTH))
        splitPane.dividerLocation = location
        descEditorWidth = editorWidth
    }

    // ─── search ──────────────────────────────────────────────────────────────

    private fun initSearchFilter(onApplied: (() -> Unit)? = null) {
        restoreSearchState()
        keywordMatcher.setKeyword(inputKeyword.text)
        groupMatcher.setKeyword(inputGroup.text)
        setSearchFilter(onApplied)
        setSearchEvent()
    }

    /**
     * read the search inputs on the event dispatch thread, evaluate the links in the background, then apply
     * the result. a newer search cancels the running one, so only the latest result is ever applied.
     */
    private fun setSearchFilter(onApplied: (() -> Unit)? = null) {
        val keywordTokens  = inputKeyword.tokens()
        val groupTokens    = inputGroup.tokens()
        val hasKeywordText = inputKeyword.text.isNotBlank()
        val hasGroupText   = inputGroup.text.isNotBlank()

        searchJob?.cancel()

        if(keywordTokens.isEmpty() && !hasKeywordText && groupTokens.isEmpty() && !hasGroupText) {
            searchJob = null
            linkService.links.predicate = { true }
            printSearchResult()
            onApplied?.invoke()
            return
        }

        val matches: (Link) -> Boolean = { link ->
            val keywordHaystack = listOf(link.title) + link.hashtag
            val inKeyword = when {
                keywordTokens.isNotEmpty() -> matchesSearchTokens(keywordHaystack, keywordTokens)
                hasKeywordText -> keywordMatcher.isMatch(keywordHaystack)
                else -> true
            }
            inKeyword && when {
                groupTokens.isNotEmpty() -> matchesSearchTokens(link.group, groupTokens)
                hasGroupText -> groupMatcher.isMatch(link.group)
                else -> true
            }
        }

        val snapshot = ArrayList(linkService.links.items)
        searchJob = launchUi {
            val evaluated = HashSet<Long>(snapshot.size)
            val matched   = HashSet<Long>()
            withContext(Dispatchers.Default) {
                for(link in snapshot) {
                    ensureActive()
                    evaluated.add(link.id)
                    if(matches(link)) matched.add(link.id)
                }
            }
            // links added after the snapshot are evaluated inline, so they are never hidden silently
            linkService.links.predicate = { link ->
                if(link.id in evaluated) link.id in matched else matches(link)
            }
            printSearchResult()
            onApplied?.invoke()
        }
    }

    private fun setSearchEvent() {

        inputGroup.onPlainEnter = {
            inputGroup.commitPendingTextNextToChips()
            handleSearchEnter()
        }
        // a chip is a committed condition, so apply it right away; plain text still waits for Enter
        inputKeyword.onTokensChanged = { submitSearch() }
        inputGroup.onTokensChanged = { submitSearch() }

        applySearchHistory(inputKeyword) { compactSearchHistory(Context.config.historySearch) }
        applyAutoCompletion(inputGroup.input, { allGroupTokenSuggestions(inputGroup.currentTermValues()) })
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

    private fun submitSearch(saveKeywordHistory: Boolean = false, onApplied: (() -> Unit)? = null) {
        keywordMatcher.setKeyword(inputKeyword.text)
        groupMatcher.setKeyword(inputGroup.text)
        setSearchFilter(onApplied)
        if(saveKeywordHistory) {
            saveSubmittedKeyword()
        }
    }

    private fun handleSearchEnter(saveKeywordHistory: Boolean = false) {
        val snapshot = searchSubmitSnapshot()
        when(searchSubmitTracker.nextAction(snapshot)) {
            SearchSubmitAction.SUBMIT -> {
                submitSearch(saveKeywordHistory) {
                    if(snapshot.isBlank() && tableMain.visibleRows in 1..10) {
                        runFirstSearchResult()
                    }
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
        if(tableModel.rowCount == 0) return
        val link = tableModel.getRowObject(0)
        tableMain.select(0)
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

    /**
     * Alt+Down lists the matching history, newest first. Alt+Left / Alt+Right step through it.
     */
    private fun applySearchHistory(
        field: SearchTokenField,
        suggestions: () -> HistorySet<SearchFieldState>,
    ) {
        var history: HistorySet<SearchFieldState>? = null
        val completion = AutoCompletion<SearchFieldState>(field.input, { it.displayText() }) { state ->
            history = null
            field.setSearchState(state)
        }
        field.input.addKeyListener(object: KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if(e.isConsumed || !e.isAltDown) return
                when(e.keyCode) {
                    KeyEvent.VK_DOWN -> {
                        val text = field.text.trim()
                        completion.show(suggestions().toList().reversed().filter {
                            text.isEmpty() || it.displayText().contains(text, ignoreCase = true)
                        })
                    }
                    KeyEvent.VK_LEFT  -> (history ?: suggestions().also { history = it }).prev()?.let { field.setSearchState(it) }
                    KeyEvent.VK_RIGHT -> (history ?: suggestions().also { history = it }).next()?.let { field.setSearchState(it) }
                    else -> return
                }
                e.consume()
            }
        })
    }

    /**
     * Alt+Down lists the suggestions matching the text. Alt+Left / Alt+Right step through them.
     */
    private fun applyAutoCompletion(
        textField: JTextField,
        suggestions: () -> HistorySet<String>,
        onAutoCompleted: (() -> Unit)? = null,
    ) {
        var suggestion: HistorySet<String>? = null
        val completion = AutoCompletion<String>(textField) {
            suggestion = null
            textField.text = it
            onAutoCompleted?.invoke()
        }
        textField.addKeyListener(object: KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if(e.isConsumed) return
                if(e.keyCode == KeyEvent.VK_ESCAPE) {
                    suggestion = null
                    return
                }
                if(!e.isAltDown) return
                when(e.keyCode) {
                    KeyEvent.VK_DOWN -> {
                        val text = textField.text.trim()
                        completion.show(suggestions().toList().filter { text.isEmpty() || it.contains(text, ignoreCase = true) })
                    }
                    KeyEvent.VK_LEFT  -> (suggestion ?: suggestions().also { suggestion = it }).prev()?.let { textField.text = it }
                    KeyEvent.VK_RIGHT -> (suggestion ?: suggestions().also { suggestion = it }).next()?.let { textField.text = it }
                    else -> return
                }
                e.consume()
            }
        })
    }

    /** suggestions follow every edit of the text. */
    private fun applyLiveAutoCompletion(
        textField: JTextField,
        suggestions: () -> HistorySet<String>,
        onAutoCompleted: (() -> Unit)? = null,
    ) {
        AutoCompletion<String>(textField) {
            textField.text = it
            onAutoCompleted?.invoke()
        }.live { text ->
            suggestions().toList().filter { it.contains(text, ignoreCase = true) }
        }
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

    // ─── icon ────────────────────────────────────────────────────────────────

    private fun changeIcon() {
        if( detail == null ) return
        linkService.openIconPicker()?.let { changeIcon(it.toFile()) }
    }

    /** show the icon of [file] in the editor. the link takes it on save. */
    private fun changeIcon(file: File) {
        if( detail == null ) return
        Link.readIcon(file)?.let { icon ->
            setDetailIcon(icon)
            buttonSave.isEnabled = true
        }
    }

    private fun setDetailIcon(image: BufferedImage?) {
        detailIcon = image
        descIcon.icon = image?.let { scaledIcon(it, DETAIL_ICON_SIZE) }
    }

    private fun copyIconToClipboard(link: Link) {
        link.iconImage?.let { image ->
            Desktop.clipboard.set(image)
            printStatus("msg.success.copy.icon".message())
        } ?: run {
            Dialog.alert("msg.error.no.icon".message())
        }
    }

    private fun saveIconToFile(link: Link) {
        link.iconImage?.let { image ->
            try {
                // initial location: the executable's directory, else the desktop, else the home directory
                val initialDir = when {
                    !link.path.isNullOrBlank() -> File(link.path!!).parentFile?.absolutePath
                    else -> null
                } ?: System.getProperty("user.home")?.let { File(it, "Desktop").absolutePath }
                ?: System.getProperty("user.home")

                val fileName = (link.title?.take(50)?.replace("[/\\\\:*?\"<>|]".toRegex(), "_") ?: "icon") + ".jpg"

                val selectedFile = linkService.openIconSavePicker(initialDir.toPath(), fileName) ?: return

                // JPEG has no alpha channel: blend the transparent parts onto white
                ImageIO.write(image.removeAlpha(Color.WHITE), "jpg", selectedFile.toFile())
                printStatus("msg.success.save.icon".message().format(selectedFile))
            } catch (e: Exception) {
                logger.error(e)
                Dialog.alert("msg.error.save.icon".message().format(e.message))
            }
        } ?: run {
            Dialog.alert("msg.error.no.icon".message())
        }
    }

    private fun showIconContextMenu(event: MouseEvent) {
        if (detail == null) return
        JPopupMenu().apply {
            add(JMenuItem("contextmenu.icon.copy".message()).apply {
                addActionListener { detail?.let { copyIconToClipboard(it) } }
            })
            add(JMenuItem("contextmenu.icon.save".message()).apply {
                addActionListener { detail?.let { saveIconToFile(it) } }
            })
        }.show(descIcon, event.x, event.y)
    }

    // ─── detail ──────────────────────────────────────────────────────────────

    private fun clearDetail() {
        detail = null
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
        setDetailIcon(null)
        // reset button status
        buttonNew.isEnabled    = true
        buttonDelete.isEnabled = true
        buttonCopy.isEnabled   = true
        buttonSave.isEnabled   = true
    }

    fun drawDetail(link: Link?) {
        if( link == null || detail === link || (link.id > 0 && detail?.id == link.id) ) return
        detail = link
        with(link) {
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
            setDetailIcon(iconImage)
        }
        listOf(descDescription, descCmdPrev, descCmdNext).forEach { it.caretPosition = 0 }
        buttonNew.isEnabled    = true
        buttonDelete.isEnabled = true
        buttonCopy.isEnabled   = true
        buttonSave.isEnabled   = false
    }

    fun drawDetailForAdd(link: Link?) {
        // expand editor
        menuViewDesc.isSelected = true

        drawDetail(link)
        buttonNew.isEnabled    = true
        buttonDelete.isEnabled = false
        buttonCopy.isEnabled   = false
        buttonSave.isEnabled   = true
        descGroupName.focusInput()
    }

    fun deleteLink(link: Link?) {

        if( link == null ) return

        val group = formatTokens(link.group)
        val summary = if( group.isNotEmpty() ) "[$group] ${link.title}" else "${link.title}"

        if( ! Dialog.confirm("msg.confirm.delete".message().format(summary)) ) return

        val prev = tableMain.selectedRow

        launchUi {
            linkService.delete(link)
            clearDetail()
            if(tableMain.rowCount > 0) tableMain.select(prev.coerceIn(0, tableMain.rowCount - 1))
            printSearchResult()
        }

    }

    fun saveDetail() {
        val link = detail ?: return

        link.title         = descTitle.text?.trim()
        link.hashtag       = descHashtag.getTokens()
        link.showConsole   = descShowConsole.isSelected
        link.executeEach   = descSeqExecution.isSelected
        link.group         = descGroupName.getTokens()
        link.description   = descDescription.text?.trim()
        link.path          = descExecPath.text?.trim()
        link.argument      = descArg.text?.trim()
        link.commandPrefix = descCmdPrefix.text?.trim()
        link.commandPrev   = descCmdPrev.text
        link.commandNext   = descCmdNext.text
        if(detailIcon !== link.iconImage) link.iconImage = detailIcon

        buttonDelete.isEnabled = true
        buttonCopy.isEnabled   = true
        buttonSave.isEnabled   = false

        launchUi {
            linkService.save(link)
            tableMain.selectBy(link)
            tableMain.scrollBy(link, ScrollMode.OUTBOUND_TO_MIDDLE)
            printSearchResult()
            printStatus("msg.alert.save.link".message())
        }
    }

    fun copyDetail() {
        val link = detail?.takeIf { it.id > 0 } ?: return
        drawDetail( link.clone() )
        descTitle.requestFocusInWindow()
        printStatus("msg.alert.copy.link".message())
        buttonDelete.isEnabled = false
        buttonCopy.isEnabled   = false
        buttonSave.isEnabled   = true
    }

    fun createDetail() {
        drawDetail(Link(icon = ICON_NEW))
        descGroupName.focusInput()
        printStatus("msg.alert.create.link".message())
        buttonDelete.isEnabled = false
        buttonCopy.isEnabled   = false
        buttonSave.isEnabled   = true
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

    // ─── table rendering ─────────────────────────────────────────────────────

    /** title with the link's icon. scaled icons are cached by the icon bytes they come from. */
    private class TitleRenderer: DefaultTableCellRenderer() {

        private val icons = WeakHashMap<ByteArray, Icon>()

        init {
            iconTextGap = 5
        }

        override fun getTableCellRendererComponent(table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
            super.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column)
            val link = value as? Link
            text = link?.title ?: ""
            icon = link?.let { tableIcon(it) }
            return this
        }

        private fun tableIcon(link: Link): Icon? {
            val bytes = link.icon ?: return null
            icons[bytes]?.let { return it }
            val icon = link.iconImage?.let { scaledIcon(it, TABLE_ICON_SIZE) } ?: return null
            icons[bytes] = icon
            return icon
        }

    }

}

private fun textArea() = JTextArea(3, 20).apply {
    lineWrap = true
    wrapStyleWord = true
}

// GridBagLayout switches every cell to its minimum size once the panel is narrower than the preferred width,
// and a text field without columns prefers the width of its text, so a long path would change the row heights
private fun detailTextField() = JTextField(1)

private fun scroll(component: JComponent) = JScrollPane(component).apply {
    preferredSize = Dimension(100, 60)
    minimumSize = Dimension(50, 40)
}

private fun button(key: String) = JButton(key.message()).apply {
    isFocusable = false
    margin = Insets(2, 8, 2, 8)
}
