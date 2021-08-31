package com.github.nayasis.kotlin.javafx.property

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.nayasis.kotlin.javafx.control.basic.allStyleables
import com.github.nayasis.kotlin.javafx.scene.previousZoomSize
import com.github.nayasis.kotlin.javafx.stage.BoundaryChecker
import javafx.css.Styleable
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.Pane
import javafx.stage.Stage
import mu.KotlinLogging
import org.controlsfx.control.CheckComboBox
import java.io.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

private val logger = KotlinLogging.logger{}

private const val PREFIX_ID = "_tmp_id"
private var seq = 0

data class StageProperty(
    val inset: InsetProperty = InsetProperty(),
    var maximized: Boolean = false,
    var previousZoomSize: InsetProperty? = null,
    val tables: HashMap<String,TableProperty> = hashMapOf(),
    val checks: HashMap<String,Boolean> = hashMapOf(),
    val values: HashMap<String,String> = hashMapOf(),
    val lists: HashMap<String,List<Int>> = hashMapOf(),
    val visibles: HashMap<String,Boolean> = hashMapOf(),
    val editables: HashMap<String,Boolean> = hashMapOf(),
    val disables: HashMap<String,Boolean> = hashMapOf(),
    val indices: HashMap<String,Int> = hashMapOf(),
): Serializable{

    @JsonIgnore
    var includeKlass = ArrayList<KClass<out Styleable>>()
    @JsonIgnore
    var excludeKlass = ArrayList<KClass<out Styleable>>()
    @JsonIgnore
    var includeId = HashSet<String>()
    @JsonIgnore
    var excludeId = HashSet<String>()
    @JsonIgnore
    val includeObject = HashSet<Styleable>()
    @JsonIgnore
    val excludeObject = HashSet<Styleable>()

    constructor(stage: Stage): this() {
        read(stage)
    }

    fun read(stage: Stage) {

        inset.read(stage)
        maximized = stage.isMaximized
        previousZoomSize = stage.scene.previousZoomSize

        getAllChildren(stage).forEach {
            if( skippable(it) ) return@forEach
            when(it) {
                is TableView<*> -> tables[it.id] = TableProperty(it)
                is CheckMenuItem -> checks[it.id] = it.isSelected
                is CheckBox -> checks[it.id] = it.isSelected
                is TextField -> values[it.id] = it.text ?: ""
                is TextArea -> values[it.id] = it.text ?: ""
                is ComboBox<*> -> indices[it.id] = it.selectionModel.selectedIndex
                is ChoiceBox<*> -> indices[it.id] = it.selectionModel.selectedIndex
                is CheckComboBox<*> -> lists[it.id] = it.checkModel.checkedIndices.toList()
            }
            when(it) {
                is Pane -> visibles[it.id] = it.isVisible
                is Control -> {
                    visibles[it.id] = it.isVisible
                    disables[it.id] = it.isDisable
                    if (it is TextInputControl) {
                        editables[it.id] = it.isEditable
                    }
                }
            }
        }

    }

    @Suppress("UNCHECKED_CAST")
    fun bind(stage: Stage?, visibility: Boolean = true ) {

        if( stage?.scene == null ) return

        inset.bind(stage)
        stage.scene.previousZoomSize = previousZoomSize

        getAllChildren(stage).forEach {
            if( skippable(it) ) return@forEach
            when(it) {
                is TableView<*> -> tables[it.id]?.bind(it as TableView<Any>)
                is CheckMenuItem -> checks[it.id]?.let{ value -> it.isSelected = value }
                is CheckBox -> checks[it.id]?.let{ value -> it.isSelected = value }
                is TextField -> values[it.id]?.let{ value -> it.text = value }
                is TextArea -> values[it.id]?.let{ value -> it.text = value }
                is ComboBox<*> -> indices[it.id]?.let{ value -> it.selectionModel.select(min(max(value,0),it.items.size-1)) }
                is ChoiceBox<*> -> indices[it.id]?.let{ value -> it.selectionModel.select(min(max(value,0),it.items.size-1)) }
                is CheckComboBox<*> -> lists[it.id]?.let{ value ->
                    it.checkModel.clearChecks()
                    it.checkModel.checkedIndices.addAll(value)
                }
            }
            if( visibility ) {
                when(it) {
                    is Pane -> visibles[it.id]?.let { value -> it.isVisible = value }
                    is Control -> {
                        visibles[it.id]?.let { value -> it.isVisible = value }
                        disables[it.id]?.let { value -> it.isDisable = value }
                        if (it is TextInputControl) {
                            editables[it.id]?.let { value -> it.isEditable = value }
                        }
                    }
                }
            }
        }

    }

    private fun getAllChildren(stage: Stage): List<Styleable> {
        return stage.scene.root?.allStyleables?.filter { setFxId(it) } ?: emptyList()
    }

    private fun setFxId(node: Styleable): Boolean {
        return if( node.id.isNullOrEmpty() ) try {
            when (node) {
                is Node -> node.id = "${PREFIX_ID}_${seq++}"
                is MenuItem -> node.id = "${PREFIX_ID}_${seq++}"
                else -> return false
            }
            true
        } catch (e: Exception) {
            seq--
            false
        } else true
    }

    private fun skippable(node: Styleable): Boolean {
        if( excludeKlass.isNotEmpty() && excludeKlass.any { it::class.isSuperclassOf(node::class) } ) return true
        if( includeKlass.isNotEmpty() && ! includeKlass.any { it::class.isSuperclassOf(node::class) } ) return true
        if( excludeId.isNotEmpty() && node.id in excludeId) return true
        if( includeId.isNotEmpty() && node.id !in includeId) return true
        if( excludeObject.isNotEmpty() && node in excludeObject) return true
        if( includeObject.isNotEmpty() && node !in includeObject) return true
        return false
    }

}