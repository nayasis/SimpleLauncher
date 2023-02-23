package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.core.string.toResource
import com.github.nayasis.kotlin.basica.reflection.Reflector
import com.github.nayasis.kotlin.javafx.misc.Desktop
import com.github.nayasis.kotlin.javafx.misc.set
import javafx.beans.property.ReadOnlyIntegerWrapper
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.web.WebView
import mu.KotlinLogging
import netscape.javascript.JSObject
import tornadofx.runLater
import java.io.Reader
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

