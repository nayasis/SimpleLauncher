// Copyright 2020 Kalkidan Betre Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.nayasis.sample.swing.decoratedUi.customjframe

import com.github.nayasis.sample.swing.decoratedUi.customdecoration.CustomDecorationParameters
import com.github.nayasis.sample.swing.decoratedUi.customdecoration.CustomDecorationWindowProc
import com.github.nayasis.sample.swing.decoratedUi.theme.Theme
import com.github.nayasis.sample.swing.decoratedUi.usercontrols.ButtonType
import com.github.nayasis.sample.swing.decoratedUi.usercontrols.ControlBoxJButton
import com.github.nayasis.sample.swing.decoratedUi.usercontrols.IconJPanel
import com.sun.jna.Native
import com.sun.jna.platform.win32.WinDef.HWND
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.Image
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JFrame
import javax.swing.JPanel

class CustomJFrame(val theme: Theme, title: String?): JFrame(title) {
    val windowProcEx: CustomDecorationWindowProc
    private var windowFrameType = WindowFrameType.NORMAL
    private var titleBar: JPanel? = null
    private var titleBarCustomContent: JPanel? = null
    private var controlBox: JPanel? = null
    private var frameContentPane: JPanel? = null
    private var iconContainer: JPanel? = null
    private var closeBtn: ControlBoxJButton? = null
    private var minimizeBtn: ControlBoxJButton? = null
    var restoreButton: ControlBoxJButton? = null
        private set
    private var closeBtnMouseAdapter: MouseAdapter? = null
    private var restoreBtnMouseAdapter: MouseAdapter? = null
    private var minimizeBtnMouseAdapter: MouseAdapter? = null

    constructor(theme: Theme, title: String?, windowFrameType: WindowFrameType): this(theme, title) {
        this.windowFrameType = windowFrameType
        initializeFrame()
    }

    override fun setVisible(b: Boolean) {
        super.setVisible(b)
        windowProcEx.init(hwnd)
    }

    private val hwnd: HWND
        private get() {
            val hwnd = HWND()
            hwnd.pointer = Native.getComponentPointer(this)
            return hwnd
        }

    private fun initializeFrame() {
        layout = BorderLayout()
        frameContentPane = JPanel()
        frameContentPane!!.layout = BorderLayout()
        frameContentPane!!.isOpaque = false
        setupFrameTitleBar()
        val clientContentPane = JPanel()
        clientContentPane.layout = FlowLayout()
        clientContentPane.isOpaque = false
        frameContentPane!!.add(clientContentPane)
        contentPane = frameContentPane
        background = theme.defaultBackgroundColor
        addComponentListener(object: ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                super.componentResized(e)
                val mainFrame = e.source as CustomJFrame
                if (mainFrame.extendedState == MAXIMIZED_BOTH) {
                    CustomDecorationParameters.maximizedWindowFrameThickness=(12)
                    mainFrame.getRootPane().border = BorderFactory.createLineBorder(
                        mainFrame.background,
                        CustomDecorationParameters.maximizedWindowFrameThickness
                    )
                    if (mainFrame.restoreButton != null) {
                        mainFrame.restoreButton!!.controlBoxButtonType = (ButtonType.RESTORE)
                    }
                } else {
                    CustomDecorationParameters.maximizedWindowFrameThickness = (0)
                    mainFrame.getRootPane().border = BorderFactory.createLineBorder(
                        theme.frameBorderColor,
                        CustomDecorationParameters.frameBorderThickness
                    )
                    if (mainFrame.restoreButton != null && mainFrame.restoreButton!!.controlBoxButtonType !== ButtonType.MAXIMIZE) {
                        mainFrame.restoreButton!!.controlBoxButtonType=(ButtonType.MAXIMIZE)
                    }
                }
            }
        })
        pack()
        CustomDecorationParameters.controlBoxWidth = (controlBox!!.width + 10)
    }

    private fun setupFrameTitleBar() {
        if (windowFrameType === WindowFrameType.NONE) {
            CustomDecorationParameters.titleBarHeight = (0)
        } else {
            titleBar = JPanel()
            titleBar!!.layout = BorderLayout()
            titleBar!!.isOpaque = false
            iconContainer = JPanel()
            iconContainer!!.isOpaque = false
            setupFrameControlBox()
            titleBar!!.add(controlBox, BorderLayout.EAST)
            titleBarCustomContent = JPanel()
            titleBarCustomContent!!.layout = FlowLayout(3, 0, 0)
            titleBarCustomContent!!.isOpaque = false
            titleBarCustomContent!!.add(iconContainer)
            titleBar!!.add(titleBarCustomContent, BorderLayout.WEST)
            frameContentPane!!.add(titleBar, BorderLayout.NORTH)
        }
    }

    private fun setupFrameControlBox() {
        controlBox = JPanel()
        controlBox!!.isOpaque = false
        if (windowFrameType === WindowFrameType.NORMAL) {
            controlBox!!.layout = GridLayout(1, 3, -1, 0)
            addMinimizeButton()
            addRestoreButton()
            addCloseButton()
        } else if (windowFrameType === WindowFrameType.TOOL) {
            controlBox!!.layout = GridLayout(1, 1, -1, 0)
            addCloseButton()
        }
    }

    private fun addCloseButton() {
        closeBtn = ControlBoxJButton(ButtonType.CLOSE, theme)
        closeBtn!!.preferredSize = Dimension(50, CustomDecorationParameters.titleBarHeight)
        closeBtn!!.background = theme.defaultBackgroundColor
        closeBtnMouseAdapter = object: MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                System.exit(0)
            }
        }
        closeBtn!!.addMouseListener(closeBtnMouseAdapter)
        controlBox!!.add(closeBtn)
    }

    private fun addRestoreButton() {
        restoreButton = ControlBoxJButton(ButtonType.MAXIMIZE, theme)
        restoreButton!!.preferredSize = Dimension(50, CustomDecorationParameters.titleBarHeight)
        restoreButton!!.background = theme.defaultBackgroundColor
        restoreBtnMouseAdapter = object: MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                extendedState = if (extendedState == MAXIMIZED_BOTH) NORMAL else MAXIMIZED_BOTH
            }
        }
        restoreButton!!.addMouseListener(restoreBtnMouseAdapter)
        controlBox!!.add(restoreButton)
    }

    private fun addMinimizeButton() {
        minimizeBtn = ControlBoxJButton(ButtonType.MINIMIZE, theme)
        minimizeBtn!!.preferredSize = Dimension(50, CustomDecorationParameters.titleBarHeight)
        minimizeBtn!!.background = theme.defaultBackgroundColor
        minimizeBtnMouseAdapter = object: MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                extendedState = ICONIFIED
            }
        }
        minimizeBtn!!.addMouseListener(minimizeBtnMouseAdapter)
        controlBox!!.add(minimizeBtn)
    }

    fun addUserControlsToTitleBar(component: Component?) {
        titleBarCustomContent!!.add(component)
        pack()
        CustomDecorationParameters.extraLeftReservedWidth = (titleBarCustomContent!!.width + 10)
    }

    val titleBarHeight: Int
        get() {
            pack()
            return titleBarCustomContent!!.height
        }

    fun setIcon(image: Image?) {
        iconContainer!!.layout = FlowLayout(1, 0, 0)
        iconContainer!!.preferredSize = Dimension(
            CustomDecorationParameters.iconWidth,
            CustomDecorationParameters.titleBarHeight
        )
        val iconJPanel = IconJPanel(image)
        iconContainer!!.add(iconJPanel)
        pack()
        CustomDecorationParameters.extraLeftReservedWidth = titleBarCustomContent!!.width + 10
    }

    fun addJFrameCloseEventAdapter(mouseAdapter: MouseAdapter?) {
        if (closeBtn != null) {
            closeBtn!!.removeMouseListener(closeBtnMouseAdapter)
            closeBtn!!.addMouseListener(mouseAdapter)
        }
    }

    fun addJFrameRestoreEventAdapter(mouseAdapter: MouseAdapter?) {
        if (restoreButton != null) {
            restoreButton!!.removeMouseListener(closeBtnMouseAdapter)
            restoreButton!!.addMouseListener(mouseAdapter)
        }
    }

    fun addJFrameMinimizeEventAdapter(mouseAdapter: MouseAdapter?) {
        if (restoreButton != null) {
            closeBtn!!.removeMouseListener(closeBtnMouseAdapter)
            closeBtn!!.addMouseListener(mouseAdapter)
        }
    }

    init {
        windowProcEx = CustomDecorationWindowProc()
        initializeFrame()
    }
}
