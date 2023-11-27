package com.github.nayasis.swing

import mdlaf.MaterialLookAndFeel
import mdlaf.animation.MaterialUIMovement
import mdlaf.themes.JMarsDarkTheme
import mdlaf.themes.MaterialLiteTheme
import mdlaf.themes.MaterialOceanicTheme
import mdlaf.utils.MaterialColors
import mdlaf.utils.MaterialImageFactory
import mdlaf.utils.MaterialManagerListener
import mdlaf.utils.icons.MaterialIconFont
import org.jdesktop.swingx.JXTaskPane
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeModel
import kotlin.math.pow


class MaterialUISwingDemo {
    init {
        beforeUsedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    }

    companion object {
        private var beforeUsedMem = 0L
        @JvmStatic
        fun main(args: Array<String>) {
            /* Not run Why? */
            // java.awt.EventQueue.invokeLater(new Runnable() {
            SwingUtilities.invokeLater {
                try {
                    JDialog.setDefaultLookAndFeelDecorated(true)
                    JFrame.setDefaultLookAndFeelDecorated(false)
                    val material = MaterialLookAndFeel(MaterialLiteTheme())
                    UIManager.setLookAndFeel(material)

                    /* DEMO */
                    val frame = JFrame("Material Design UI for Swing by atharva washimkar ♥")
                    frame.minimumSize = Dimension(600, 400)
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
                    // Test for fix the issue
                    // https://github.com/vincenzopalazzo/material-ui-swing/projects/1#card-21599924
                    // frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    val bar = JMenuBar()
                    val surrogates = intArrayOf(0xd83d, 0xde00)
                    val alienEmojiString = String(surrogates, 0, surrogates.size)
                    println("\u263a")
                    val menu1 = JMenu("\u263a")
                    // menu1.setFont(new FontUIResource(Font.DIALOG, Font.BOLD, 12));
                    // menu1.setFont(new
                    // MaterialWrapperFont(MaterialFontFactory.getInstance().getFont(MaterialFontFactory.BOLD).deriveFont(25f)));
                    val menu2 = JMenu("Option 2 ♥")

                    class ActionTestJFC(var component: JComponent): AbstractAction() {
                        init {
                            putValue(Action.NAME, "Test JFileChooser (Animated)")
                            putValue(Action.SHORT_DESCRIPTION, "Test JFileChooser")
                        }

                        override fun actionPerformed(e: ActionEvent?) {
                            val fileChooser = JFileChooser()
                            fileChooser.showDialog(component, "Test OK")
                        }
                    }

                    val menuTheme = JMenu("Themes")
                    val oceanic = JMenuItem()
                    oceanic.setAction(
                        object: AbstractAction("Material Oceanic") {
                            override fun actionPerformed(e: ActionEvent?) {
                                println("********")
                                MaterialLookAndFeel.changeTheme(MaterialOceanicTheme())
                                SwingUtilities.updateComponentTreeUI(frame)
                            }
                        })
                    val lite = JMenuItem()
                    lite.setAction(
                        object: AbstractAction("Material Lite") {
                            override fun actionPerformed(e: ActionEvent?) {
                                MaterialLookAndFeel.changeTheme(MaterialLiteTheme())
                                SwingUtilities.updateComponentTreeUI(frame)
                            }
                        })
                    val jmarsDark = JMenuItem()
                    jmarsDark.setAction(
                        object: AbstractAction("Material JMars Dark") {
                            override fun actionPerformed(e: ActionEvent?) {
                                MaterialLookAndFeel.changeTheme(JMarsDarkTheme())
                                SwingUtilities.updateComponentTreeUI(frame)
                            }
                        })
                    menuTheme.add(oceanic)
                    menuTheme.add(lite)
                    menuTheme.add(jmarsDark)
                    bar.add(menuTheme)
                    val item1 = JMenuItem("Item 1 (Animated)")
                    item1.setAction(ActionTestJFC(JPanel()))
                    val item2 = JMenuItem("Item 2 (Not animated)")

                    // Test RadioButtonMenuItem
                    val jRadioButtonMenuItem = JRadioButtonMenuItem()
                    jRadioButtonMenuItem.setText("test RadioButtonMenuItem")
                    menu1.add(jRadioButtonMenuItem)
                    menu1.addSeparator()
                    // TestCheckBoxMenuItem
                    val checkBoxMenuItem = JCheckBoxMenuItem()
                    checkBoxMenuItem.setText("test")
                    menu1.add(checkBoxMenuItem)
                    menu1.addSeparator()
                    menu1.add(item1)
                    menu2.add(item2)
                    val menuItemTestUno = JMenuItem("Test distance")
                    val menuItemTestDue = JMenuItem("Test distance")
                    val menuItemExit = JMenuItem("Exit")
                    menu1.addSeparator()
                    menu1.add(menuItemExit)
                    menu2.add(menuItemTestUno)
                    menu2.add(checkBoxMenuItem)
                    menu2.add(menuItemTestDue)
                    menu2.add(jRadioButtonMenuItem)
                    bar.add(menu1)
                    bar.add(menu2)

                    // configuring a simple JButton
                    val button = JButton("I'm Disabled")
                    button.setBackground(MaterialColors.COSMO_RED)
                    button.addMouseListener(
                        MaterialUIMovement.getMovement(button, MaterialColors.YELLOW_400)
                    )
                    button.setEnabled(false)
                    val content = JPanel()
                    content.add(button)
                    class ActionEnableButton(button: JButton): AbstractAction() {
                        private val button: JButton

                        init {
                            putValue(Action.NAME, "I can enable")
                            this.button = button
                        }

                        override fun actionPerformed(e: ActionEvent?) {
                            if (button.isEnabled) {
                                button.setEnabled(false)
                                button.setText("I'm disable")
                            } else {
                                button.setEnabled(true)
                                button.setText("I'm enable")
                            }
                        }
                    }

                    val abiliteButton = JButton("I can enable")
                    abiliteButton.setAction(ActionEnableButton(button))
                    abiliteButton.setBackground(MaterialColors.COSMO_BLUE)
                    abiliteButton.setForeground(MaterialColors.WHITE)
                    abiliteButton.addMouseListener(
                        MaterialUIMovement.getMovement(abiliteButton, MaterialColors.COSMO_LIGTH_BLUE)
                    )
                    content.add(abiliteButton)

                    // Test a MaterialTitleBorder
                    val materialTitleBorder = TitledBorder("Test Border")
                    content.setBorder(materialTitleBorder)

                    // add everything to the frame
                    frame.add(bar, BorderLayout.PAGE_START)
                    // frame.add (content, BorderLayout.CENTER);

                    // Deprecated, now the library add inside the class the event
                    // MaterialUIMovement.add(menu1, MaterialColors.GRAY_200);
                    // MaterialUIMovement.add(item1, MaterialColors.GRAY_200);

                    // you can also pass in extra parameters indicating how many intermediate colors to
                    // display, as well as the "frame rate" of the animation
                    // there will be 5 intermediate colors displayed in the transition from the original
                    // components color to the new one specified
                    // the "frame rate" of the transition will be 1000 / 30, or 30 FPS
                    // the animation will take 5 * 1000 / 30 = 166.666... milliseconds to complete
                    // MaterialUIMovement.add(button, MaterialColors.LIGHT_BLUE_500, 5, 1000 / 30);

                    //
                    content.add(JCheckBox("checkbox"))
                    val combo = JComboBox(arrayOf("Pizza", "Pasta", "Sushi"))
                    // combo.setEnabled(false);
                    content.add(combo)
                    content.add(JLabel("label"))
                    content.add(JPasswordField("password"))
                    content.add(JRadioButton("radio button"))
                    val slider = JSlider(JSlider.HORIZONTAL, 0, 5, 2)
                    slider.setEnabled(true)
                    slider.setFocusable(true)
                    content.add(slider)
                    content.add(JSpinner(SpinnerListModel(arrayOf("d", "e", "f"))))
                    content.add(
                        JTable(
                            arrayOf(
                                arrayOf("a", "b", "c"),
                                arrayOf("d", "e", "f")
                            ), arrayOf("r", "e")
                        )
                    )
                    content.add(JTextField("text field U+1F600"))
                    content.add(JToggleButton("toggle"))
                    val tb = JToolBar("toolbar")
                    val button1 = JButton("f")

                    class ActionTest: AbstractAction() {
                        init {
                            putValue(Action.NAME, "f")
                            putValue(Action.SHORT_DESCRIPTION, "Test tool tip")
                        }

                        override fun actionPerformed(e: ActionEvent?) {
                            val dialog = JDialog()
                            val jPanel = JPanel()
                            jPanel.add(JColorChooser())
                            dialog.contentPane = jPanel
                            dialog.setLocationRelativeTo(null)
                            dialog.isVisible = true
                            dialog.pack()
                        }
                    }
                    button1.setAction(ActionTest())
                    val button2 = JButton("e")
                    button1.setBackground(MaterialColors.LIGHT_BLUE_400)
                    button1.setForeground(Color.WHITE)
                    button2.setBackground(MaterialColors.LIGHT_BLUE_400)
                    button2.setForeground(Color.WHITE)
                    button1.addMouseListener(
                        MaterialUIMovement.getMovement(button1, MaterialColors.LIGHT_BLUE_300)
                    )
                    button2.addMouseListener(
                        MaterialUIMovement.getMovement(button2, MaterialColors.LIGHT_BLUE_300)
                    )
                    tb.add(button1)
                    tb.addSeparator()
                    tb.add(button2)
                    tb.isFloatable = true
                    content.add(tb)
                    val tNodeRoot = DefaultMutableTreeNode("Root")
                    tNodeRoot.add(DefaultMutableTreeNode("Child1"))
                    tNodeRoot.add(DefaultMutableTreeNode("Child2"))
                    val tModel: TreeModel = DefaultTreeModel(tNodeRoot)
                    val tree = JTree(tModel)
                    tree.setEditable(true)
                    content.add(tree)
                    val sp = JScrollPane(content)
                    sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS)
                    sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS)
                    val pn = JPanel()
                    val panel3 = JPanel()
                    val tp = JTabbedPane()
                    tp.setTabPlacement(JTabbedPane.RIGHT)
                    tp.addTab("bleh1", pn)
                    tp.addTab("bleh", sp)
                    tp.addTab("Panel 3", panel3)
                    frame.add(tp, BorderLayout.CENTER)

                    // test progressBar
                    var progressBar = JProgressBar()
                    progressBar.setValue(6)
                    progressBar.maximum = 12
                    pn.add(progressBar)

                    // test cange coloro maximum value progress bar
                    progressBar = JProgressBar()
                    progressBar.maximum = 5
                    progressBar.setValue(5)
                    pn.add(progressBar)
                    val textPane = JTextPane()
                    textPane.text = "Hi I'm super sayan"
                    val textPane1 = JTextPane()
                    textPane1.text = "Hi I'm super sayan"
                    textPane1.setEnabled(false)
                    pn.add(textPane)
                    pn.add(textPane1)
                    val editorPane = JEditorPane()
                    editorPane.text =
                        "I added a second character for Arabic support, it is activated according to the locale"
                    pn.add(editorPane)
                    val buttonTwoo = JButton()

                    class ActionToastTest(var component: JComponent): AbstractAction() {
                        init {
                            putValue(Action.NAME, "Test Toast")
                            putValue(Action.SHORT_DESCRIPTION, "Test Toast")
                        }

                        override fun actionPerformed(e: ActionEvent?) {
                            val pane = JOptionPane()
                            val message = """The componet toast is removed into project 
 Because it carried unnecessary dependencies, but you can find the component here
https://github.com/vincenzopalazzo/toasts-for-swing"""
                            JOptionPane.showMessageDialog(
                                frame, message, "Info on Toast ???", JOptionPane.INFORMATION_MESSAGE
                            )
                        }
                    }
                    buttonTwoo.setAction(ActionToastTest(pn))
                    buttonTwoo.setBackground(MaterialColors.PURPLE_600)
                    buttonTwoo.setForeground(MaterialColors.GRAY_100)
                    buttonTwoo.addMouseListener(
                        MaterialUIMovement.getMovement(buttonTwoo, MaterialColors.PURPLE_300)
                    )
                    pn.add(buttonTwoo)
                    val bottoneConImmagine = JButton()
                    bottoneConImmagine.setIcon(
                        MaterialImageFactory.getInstance().getImage(MaterialIconFont.HOME)
                    )
                    pn.add(bottoneConImmagine)
                    val buttonTestTextFieled = JButton("Test JtexFiele")

                    class AzioneTestJTexField: AbstractAction() {
                        init {
                            putValue(Action.NAME, "testJtextFieled")
                        }

                        override fun actionPerformed(e: ActionEvent?) {
                            val dialog = JDialog()
                            dialog.add(JComboBox<String>())
                            dialog.add(JSpinner())
                            dialog.size = Dimension(50, 50)
                            dialog.setLocationRelativeTo(frame)
                            dialog.isVisible = true
                        }
                    }
                    buttonTestTextFieled.setAction(AzioneTestJTexField())
                    pn.add(buttonTestTextFieled)
                    val jxTaskPane = JXTaskPane()
                    jxTaskPane.setTitle("Material UI memory")
                    jxTaskPane.setOpaque(false)
                    val memoryOccupedNow = JLabel()
                    jxTaskPane.add(memoryOccupedNow)

                    // Test effect mouse over
                    // Setting default
                    val testButtonHoverOne = JButton("Fly over me One")
                    testButtonHoverOne.setEnabled(false)
                    pn.add(testButtonHoverOne)

                    // ModSetting
                    val testButtonHoverTwo = JButton("Fly over me Two")
                    testButtonHoverTwo.setBackground(MaterialColors.LIGHT_BLUE_500)
                    testButtonHoverTwo.setForeground(MaterialColors.WHITE)
                    testButtonHoverTwo.addMouseListener(
                        MaterialUIMovement.getMovement(
                            testButtonHoverTwo, MaterialColors.LIGHT_BLUE_200
                        )
                    )
                    pn.add(testButtonHoverTwo)
                    pn.add(jxTaskPane)
                    // make everything visible to the world

                    // Init Jtree in panel 3
                    val style = DefaultMutableTreeNode("Style")
                    val color = DefaultMutableTreeNode("color")
                    val font = DefaultMutableTreeNode("font")
                    style.add(color)
                    style.add(font)
                    val red = DefaultMutableTreeNode("red")
                    val blue = DefaultMutableTreeNode("blue")
                    val black = DefaultMutableTreeNode("black")
                    val green = DefaultMutableTreeNode("green")
                    color.add(red)
                    color.add(blue)
                    color.add(black)
                    color.add(green)
                    val jt = JTree(style)
                    panel3.add(jt)
                    val buttonInfo = JButton("Test INFO")
                    buttonInfo.setBackground(MaterialColors.COSMO_GREEN)
                    buttonInfo.setForeground(MaterialColors.COSMO_LIGTH_GRAY)
                    buttonInfo.addMouseListener(
                        MaterialUIMovement.getMovement(buttonInfo, MaterialColors.COSMO_LIGHT_GREEN)
                    )
                    class InfoMessage: AbstractAction() {
                        init {
                            putValue(Action.NAME, "Info option panel")
                        }

                        override fun actionPerformed(e: ActionEvent?) {
                            val optionPane = JOptionPane()
                            JOptionPane.showMessageDialog(
                                frame,
                                "This is message info",
                                "Message info",
                                JOptionPane.INFORMATION_MESSAGE
                            )
                        }
                    }
                    buttonInfo.setAction(InfoMessage())
                    panel3.add(buttonInfo)
                    val buttonError = JButton()
                    buttonError.setBackground(MaterialColors.COSMO_RED)
                    buttonError.setForeground(MaterialColors.COSMO_LIGTH_GRAY)
                    buttonError.addMouseListener(
                        MaterialUIMovement.getMovement(buttonError, MaterialColors.COSMO_LIGHT_RED)
                    )
                    class ErrorMassage: AbstractAction() {
                        init {
                            putValue(Action.NAME, "Error option panel")
                        }

                        override fun actionPerformed(e: ActionEvent?) {
                            val optionPane = JOptionPane()
                            JOptionPane.showMessageDialog(
                                frame, "This is message error", "Message error", JOptionPane.ERROR_MESSAGE
                            )
                        }
                    }
                    buttonError.setAction(ErrorMassage())
                    panel3.add(buttonError)
                    val buttonQuestion = JButton()
                    buttonQuestion.setBackground(MaterialColors.COSMO_BLUE)
                    buttonQuestion.setForeground(MaterialColors.COSMO_LIGTH_GRAY)
                    buttonQuestion.addMouseListener(
                        MaterialUIMovement.getMovement(buttonQuestion, MaterialColors.COSMO_LIGTH_BLUE)
                    )
                    class QuesuionMessage: AbstractAction() {
                        init {
                            putValue(Action.NAME, "Info question panel")
                        }

                        override fun actionPerformed(e: ActionEvent?) {
                            val optionPane = JOptionPane()
                            JOptionPane.showMessageDialog(
                                frame,
                                "This is message question",
                                "Message question",
                                JOptionPane.QUESTION_MESSAGE
                            )
                        }
                    }
                    buttonQuestion.setAction(QuesuionMessage())
                    frame.rootPane.setDefaultButton(buttonQuestion)
                    val buttonWarning = JButton()
                    buttonWarning.setOpaque(false)
                    buttonWarning.setForeground(MaterialColors.COSMO_LIGTH_GRAY)
                    buttonWarning.setBackground(MaterialColors.COSMO_ORANGE)
                    buttonWarning.addMouseListener(
                        MaterialUIMovement.getMovement(buttonWarning, MaterialColors.COSMO_LIGHT_ORANGE)
                    )
                    class WarningMessage: AbstractAction() {
                        init {
                            putValue(Action.NAME, "Info warning panel")
                        }

                        override fun actionPerformed(e: ActionEvent?) {
                            val optionPane = JOptionPane()
                            JOptionPane.showMessageDialog(
                                frame,
                                "This is message warning",
                                "Message warning",
                                JOptionPane.WARNING_MESSAGE
                            )
                        }
                    }
                    buttonWarning.setAction(WarningMessage())
                    panel3.add(buttonQuestion)
                    panel3.add(buttonWarning)
                    val spinnerDate = JSpinner(SpinnerDateModel())
                    spinnerDate.setEnabled(false)
                    val spinnerNumbar = JSpinner(SpinnerNumberModel())
                    val objectList = ArrayList<Any?>()
                    objectList.add(Any())
                    objectList.add(Any())
                    objectList.add(Any())
                    val spinnerList = JSpinner(SpinnerListModel(objectList))
                    panel3.add(spinnerDate)
                    panel3.add(spinnerNumbar)
                    panel3.add(spinnerList)
                    val panel4 = JPanel()

                    // String array to store weekdays
                    val week = arrayOf(
                        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
                    )
                    val listDay = JList(week)
                    val buttonOptionPane = JButton("Click for open JOptionpane")

                    class OpenOptionPane: AbstractAction() {
                        init {
                            putValue(Action.NAME, "Click me for open JOptionpane")
                        }

                        override fun actionPerformed(e: ActionEvent?) {
                            val op = JOptionPane(
                                null, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION
                            )
                            val dialog = op.createDialog("Test Button principal")
                            // dialog.setUndecorated(true);
                            dialog.isVisible = true
                        }
                    }
                    buttonOptionPane.setAction(OpenOptionPane())
                    panel4.add(buttonOptionPane)
                    panel4.add(listDay)

                    // Test label disable
                    val labelDisable = JLabel("I'm disabled")
                    panel4.add(labelDisable)
                    labelDisable.setEnabled(false)
                    val buttonEnableLabel = JButton("Enable lable")
                    buttonEnableLabel.setBackground(MaterialColors.COSMO_BLACK)
                    buttonEnableLabel.setForeground(MaterialColors.COSMO_LIGTH_GRAY)
                    MaterialManagerListener.removeAllMaterialMouseListener(buttonEnableLabel)
                    buttonEnableLabel.addMouseListener(
                        MaterialUIMovement.getMovement(
                            buttonEnableLabel, MaterialColors.COSMO_DARK_GRAY
                        )
                    )
                    class ActionEnableLabel: AbstractAction() {
                        init {
                            putValue(Action.NAME, "Enable label")
                        }

                        override fun actionPerformed(e: ActionEvent?) {
                            if (labelDisable.isEnabled) {
                                labelDisable.setEnabled(false)
                                return
                            }
                            labelDisable.setEnabled(true)
                        }
                    }
                    buttonEnableLabel.setAction(ActionEnableLabel())
                    panel4.add(buttonEnableLabel)
                    val disabledCheckBox = JCheckBox("I'm Disabled")
                    disabledCheckBox.setEnabled(false)
                    panel4.add(disabledCheckBox)
                    val radioDisabled = JRadioButton("radio disabled")
                    radioDisabled.setEnabled(false)
                    panel4.add(radioDisabled)
                    val textFieldBugListener = JTextField(
                        "Test for bug https://github.com/vincenzopalazzo/material-ui-swing/issues/63"
                    )
                    textFieldBugListener.addActionListener { println("The bag was fixed?") }
                    panel4.add(textFieldBugListener)
                    tp.addTab("Panel 4", panel4)
                    frame.pack()
                    frame.isVisible = true
                    frame.setLocationRelativeTo(null)
                    val lastUsedMem =
                        Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
                    val megamemori: Double =
                        (lastUsedMem - beforeUsedMem) * 9.537 * 10.0.pow(-7.0)
                    memoryOccupedNow.setText("Memory occuped after update: $megamemori MB")

                    // For testing
                    // SwingUtilities.updateComponentTreeUI(frame);
                } catch (e: UnsupportedLookAndFeelException) {
                    e.printStackTrace()
                }
            }
        }
    }
}