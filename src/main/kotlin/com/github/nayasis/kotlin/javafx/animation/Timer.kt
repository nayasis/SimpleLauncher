package com.github.nayasis.kotlin.javafx.animation

import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.util.Duration

object Timer {

    fun run(time: Duration, action: EventHandler<ActionEvent>, vararg values: KeyValue, count: Int = 1 ): Timeline {
        return timeline(time, action, *values, count = count).also {
            it.playFromStart()
        }
    }

    fun timeline(time: Duration, action: EventHandler<ActionEvent>, vararg values: KeyValue, count: Int = 0 ): Timeline {
        return Timeline( KeyFrame(time,action,*values) ).apply {
            cycleCount = if(count > 0) count else Timeline.INDEFINITE
        }
    }

}