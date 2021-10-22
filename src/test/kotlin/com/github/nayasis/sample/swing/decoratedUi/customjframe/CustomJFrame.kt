// Copyright 2020 Kalkidan Betre Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.nayasis.sample.swing.decoratedUi.customjframe

import com.github.nayasis.sample.swing.decoratedUi.customdecoration.CustomDecorationParameters
import com.github.nayasis.sample.swing.decoratedUi.customdecoration.CustomDecorationWindowProc
import com.github.nayasis.sample.swing.decoratedUi.theme.Theme
import com.github.nayasis.sample.swing.decoratedUi.usercontrols.ButtonType
import com.github.nayasis.sample.swing.decoratedUi.usercontrols.ControlBoxJButton
import com.github.nayasis.sample.swing.decoratedUi.usercontrols.IconJPanel
import com.sun.jna.Native
import com.sun.jna.platform.win32.WinDef
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
    private var frameContentPane: JPanel = JPanel()
    private var iconContainer: JPanel? = null
    private var closeBtn: ControlBoxJButton? = null
    private var minimizeBtn: ControlBoxJButton? = null
    private var restoreButton: ControlBoxJButton? = null
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

    private val hwnd: WinDef.HWND
        private get() {
            val hwnd = WinDef.HWND()
            hwnd.setPointer(Native.getComponentPointer(this))
            return hwnd
        }

    fun getRestoreButton(): ControlBoxJButton? {
        return restoreButton
    }

    private fun initializeFrame() {
        setLayout(BorderLayout())
        frameContentPane.setLayout(BorderLayout())
        frameContentPane.setOpaque(false)
        setupFrameTitleBar()
        val clientContentPane = JPanel()
        clientContentPane.setLayout(FlowLayout())
        clientContentPane.setOpaque(false)
        frameContentPane.add(clientContentPane)
        setContentPane(frameContentPane)
        setBackground(theme.defaultBackgroundColor)
        addComponentListener(object: ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                super.componentResized(e)
                val mainFrame = e.getSource() as CustomJFrame
                if (mainFrame.getExtendedState() == JFrame.MAXIMIZED_BOTH) {
                    CustomDecorationParameters.maximizedWindowFrameThickness = 12
                    mainFrame.getRootPane().setBorder(
                        BorderFactory.createLineBorder(
                            mainFrame.getBackground(),
                            CustomDecorationParameters.maximizedWindowFrameThickness
                        )
                    )
                    if (mainFrame.getRestoreButton() != null) {
                        (mainFrame.getRestoreButton())?.controlBoxButtonType = ButtonType.RESTORE
                    }
                } else {
                    CustomDecorationParameters.maximizedWindowFrameThickness = 0
                    mainFrame.getRootPane().setBorder(
                        BorderFactory.createLineBorder(
                            theme.frameBorderColor,
                            CustomDecorationParameters.frameBorderThickness
                        )
                    )
                    if (mainFrame.getRestoreButton() != null && (mainFrame.getRestoreButton())?.controlBoxButtonType !== ButtonType.MAXIMIZE) {
                        (mainFrame.getRestoreButton())?.controlBoxButtonType = ButtonType.MAXIMIZE
                    }
                }
            }
        })
        pack()
        CustomDecorationParameters.controlBoxWidth = controlBox!!.getWidth() + 10
    }

    private fun setupFrameTitleBar() {
        if (windowFrameType === WindowFrameType.NONE) {
            CustomDecorationParameters.titleBarHeight = 0
        } else {
            titleBar = JPanel()
            titleBar!!.setLayout(BorderLayout())
            titleBar!!.setOpaque(false)
            iconContainer = JPanel()
            iconContainer!!.setOpaque(false)
            setupFrameControlBox()
            titleBar!!.add(controlBox, BorderLayout.EAST)
            titleBarCustomContent = JPanel()
            titleBarCustomContent!!.setLayout(FlowLayout(3, 0, 0))
            titleBarCustomContent!!.setOpaque(false)
            titleBarCustomContent!!.add(iconContainer)
            titleBar!!.add(titleBarCustomContent, BorderLayout.WEST)
            frameContentPane.add(titleBar, BorderLayout.NORTH)
        }
    }

    private fun setupFrameControlBox() {
        controlBox = JPanel()
        controlBox?.setOpaque(false)
        if (windowFrameType === WindowFrameType.NORMAL) {
            controlBox?.setLayout(GridLayout(1, 3, -1, 0))
            addMinimizeButton()
            addRestoreButton()
            addCloseButton()
        } else if (windowFrameType === WindowFrameType.TOOL) {
            controlBox?.setLayout(GridLayout(1, 1, -1, 0))
            addCloseButton()
        }
    }

    private fun addCloseButton() {
        closeBtn = ControlBoxJButton(ButtonType.CLOSE, theme)
        closeBtn?.setPreferredSize(Dimension(50, CustomDecorationParameters.titleBarHeight))
        closeBtn?.setBackground(theme.defaultBackgroundColor)
        closeBtnMouseAdapter = object: MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                System.exit(0)
            }
        }
        closeBtn?.addMouseListener(closeBtnMouseAdapter)
        controlBox?.add(closeBtn)
    }

    private fun addRestoreButton() {
        restoreButton = ControlBoxJButton(ButtonType.MAXIMIZE, theme)
        restoreButton?.setPreferredSize(Dimension(50, CustomDecorationParameters.titleBarHeight))
        restoreButton?.setBackground(theme.defaultBackgroundColor)
        restoreBtnMouseAdapter = object: MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (getExtendedState() == JFrame.MAXIMIZED_BOTH) setExtendedState(JFrame.NORMAL) else setExtendedState(
                    JFrame.MAXIMIZED_BOTH
                )
            }
        }
        restoreButton?.addMouseListener(restoreBtnMouseAdapter)
        controlBox?.add(restoreButton)
    }

    private fun addMinimizeButton() {
        minimizeBtn = ControlBoxJButton(ButtonType.MINIMIZE, theme)
        minimizeBtn?.setPreferredSize(Dimension(50, CustomDecorationParameters.titleBarHeight))
        minimizeBtn?.setBackground(theme.defaultBackgroundColor)
        minimizeBtnMouseAdapter = object: MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                setExtendedState(JFrame.ICONIFIED)
            }
        }
        minimizeBtn?.addMouseListener(minimizeBtnMouseAdapter)
        controlBox?.add(minimizeBtn)
    }

    fun addUserControlsToTitleBar(component: Component?) {
        titleBarCustomContent?.add(component)
        pack()
        CustomDecorationParameters.extraLeftReservedWidth = titleBarCustomContent!!.getWidth() + 10
    }

    val titleBarHeight: Int
        get() {
            pack()
            return titleBarCustomContent!!.height
        }

    fun setIcon(image: Image?) {
        iconContainer?.setLayout(FlowLayout(1, 0, 0))
        iconContainer?.setPreferredSize(
            Dimension(
                CustomDecorationParameters.iconWidth,
                CustomDecorationParameters.titleBarHeight
            )
        )
        val iconJPanel = IconJPanel(image)
        iconContainer?.add(iconJPanel)
        pack()
        CustomDecorationParameters.extraLeftReservedWidth = titleBarCustomContent!!.getWidth() + 10
    }

    fun addJFrameCloseEventAdapter(mouseAdapter: MouseAdapter?) {
        if (closeBtn != null) {
            closeBtn?.removeMouseListener(closeBtnMouseAdapter)
            closeBtn?.addMouseListener(mouseAdapter)
        }
    }

    fun addJFrameRestoreEventAdapter(mouseAdapter: MouseAdapter?) {
        if (restoreButton != null) {
            restoreButton?.removeMouseListener(closeBtnMouseAdapter)
            restoreButton?.addMouseListener(mouseAdapter)
        }
    }

    fun addJFrameMinimizeEventAdapter(mouseAdapter: MouseAdapter?) {
        if (restoreButton != null) {
            closeBtn?.removeMouseListener(closeBtnMouseAdapter)
            closeBtn?.addMouseListener(mouseAdapter)
        }
    }

    init {
        windowProcEx = CustomDecorationWindowProc()
        initializeFrame()
    }

}