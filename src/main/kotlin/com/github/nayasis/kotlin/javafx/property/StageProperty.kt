package com.github.nayasis.kotlin.javafx.property

import com.fasterxml.jackson.annotation.JsonIgnore
import com.sun.javafx.css.Stylesheet
import javafx.css.Styleable
import javafx.stage.Stage
import java.io.Serializable

class StageProperty(
    val size: SizeProperty = SizeProperty(),
    val position: PositionProperty = PositionProperty(),
    val maximized: Boolean = false,
    val zoomedSize: SizeProperty = SizeProperty(),
    val tables: Map<String,TableProperty> = hashMapOf(),
    val checks: Map<String,Boolean> = hashMapOf(),
    val values: Map<String,String> = hashMapOf(),
    val lists: Map<String,ArrayList<Int>> = hashMapOf(),
    val visibles: Map<String,Boolean> = hashMapOf(),
    val editables: Map<String,Boolean> = hashMapOf(),
    val disables: Map<String,Boolean> = hashMapOf(),
): Serializable{

    @JsonIgnore
    val includes: List<out Styleable> = ArrayList()
    @JsonIgnore
    val excludes: List<out Styleable> = ArrayList()

    fun read(stage: Stage) {
        size.read(stage)
        position.read(stage)
    }

}