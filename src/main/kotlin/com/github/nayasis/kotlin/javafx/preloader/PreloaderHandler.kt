package com.github.nayasis.kotlin.javafx.preloader

interface PreloaderHandler {
    fun execute(message: String?, percentage: Double?)
}