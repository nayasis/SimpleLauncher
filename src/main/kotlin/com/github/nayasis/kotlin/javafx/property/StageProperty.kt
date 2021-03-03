package com.github.nayasis.kotlin.javafx.property

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.nayasis.kotlin.javafx.scene.previousZoomSize
import com.github.nayasis.kotlin.javafx.stage.BoundaryChecker
import javafx.css.Styleable
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.Pane
import javafx.stage.Stage
import org.controlsfx.control.CheckComboBox
import tornadofx.getChildList
import java.io.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

private const val PREFIX_ID = "_tmp_id"
private var seq = 0

data class StageProperty(
    val inset: InsetProperty = InsetProperty(),
    var maximized: Boolean = false,
    var previousZoomSize: InsetProperty? = null,
    val tables: HashMap<String, TableProperty> = hashMapOf(),
    val checks: HashMap<String, Boolean> = hashMapOf(),
    val values: HashMap<String, String> = hashMapOf(),
    val lists: HashMap<String, List<Int>> = hashMapOf(),
    val visibles: HashMap<String, Boolean> = hashMapOf(),
    val editables: HashMap<String, Boolean> = hashMapOf(),
    val disables: HashMap<String, Boolean> = hashMapOf(),
    val indices: HashMap<String, Int> = hashMapOf(),
): Serializable{

    @JsonIgnore
    var includes: MutableList<KClass<out Styleable>> = java.util.ArrayList()
    @JsonIgnore
    var excludes: MutableList<KClass<out Styleable>> = java.util.ArrayList()

    fun read(stage: Stage) {

        inset.read(stage)
        maximized = stage.isMaximized
        previousZoomSize = stage.scene.previousZoomSize

        stage.scene?.root?.getChildList()?.forEach {
            if( ! allow(it) ) return
            val fxid = getFxId(it)
            when(it) {
                is TableView<*> -> tables[fxid] = TableProperty(it)
                is CheckMenuItem -> checks[fxid] = it.isSelected
                is CheckBox -> checks[fxid] = it.isSelected
                is TextField -> values[fxid] = it.text
                is TextArea -> values[fxid] = it.text
                is ComboBox<*> -> indices[fxid] = it.selectionModel.selectedIndex
                is CheckComboBox<*> -> lists[fxid] = it.checkModel.checkedIndices.toList()
            }
            when(it) {
                is Pane -> visibles[fxid] = it.isVisible
                is Control -> {
                    visibles[fxid] = it.isVisible
                    disables[fxid] = it.isDisable
                    if (it is TextInputControl) {
                        editables[fxid] = it.isEditable
                    }
                }
            }
        }

    }

    fun apply(stage: Stage?, visibility: Boolean ) {

        if( stage?.scene == null ) return

        inset.apply(stage)
        BoundaryChecker.reset(stage)
        stage.scene.previousZoomSize = previousZoomSize

        stage.scene.root?.getChildList()?.forEach {
            if( ! allow(it) ) return
            val fxid = getFxId(it)
            when(it) {
                is TableView<*> -> tables[fxid]?.apply(it as TableView<Any>)
                is CheckMenuItem -> checks[fxid]?.let{ value -> it.isSelected = value }
                is CheckBox -> checks[fxid]?.let{ value -> it.isSelected = value }
                is TextField -> values[fxid]?.let{ value -> it.text = value }
                is TextArea -> values[fxid]?.let{ value -> it.text = value }
                is ComboBox<*> -> indices[fxid]?.let{ value -> it.selectionModel.select(min(max(value,0),it.items.size-1)) }
                is ChoiceBox<*> -> indices[fxid]?.let{ value -> it.selectionModel.select(min(max(value,0),it.items.size-1)) }
                is CheckComboBox<*> -> lists[fxid]?.let{ value ->
                    it.checkModel.clearChecks()
                    it.checkModel.checkedIndices.addAll(value)
                }

            }
            if( visibility ) {
                when(it) {
                    is Pane -> visibles[fxid]?.let { value -> it.isVisible = value }
                    is Control -> {
                        visibles[fxid]?.let { value -> it.isVisible = value }
                        disables[fxid]?.let { value -> it.isDisable = value }
                        if (it is TextInputControl) {
                            editables[fxid]?.let { value -> it.isEditable = value }
                        }
                    }
                }
            }
        }

    }

    private fun getFxId(node: Styleable): String {
        if( node.id.isNullOrEmpty() ) {
            if( node is Node ) {
                node.id = "${PREFIX_ID}_${seq++}"
            } else if( node is MenuItem ) {
                node.id = "${PREFIX_ID}_${seq++}"
            }
        }
        return node.id
    }

    private fun allow(node: Styleable): Boolean {
        if( excludes.isNotEmpty() && excludes.any { it::class.isSuperclassOf(node::class) } ) return false
        if( includes.isNotEmpty() && ! includes.any { it::class.isSuperclassOf(node::class) } ) return false
        return true
    }

}