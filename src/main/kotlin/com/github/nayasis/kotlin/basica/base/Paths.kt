package com.github.nayasis.kotlin.basica

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

inline fun Path?.exists(vararg options: LinkOption = emptyArray()): Boolean = this != null && Files.exists(this, *options)

inline fun Path?.notExists(vararg options: LinkOption = emptyArray()): Boolean = this != null && Files.notExists(this, *options)

inline fun Path?.isFile(vararg options: LinkOption = emptyArray()): Boolean = this != null && Files.isRegularFile(this, *options)

inline fun Path?.isDirectory(vararg options: LinkOption = emptyArray()): Boolean = this != null && Files.isDirectory(this, *options)

inline fun Path?.extension(default: String = ""): String {
    return when {
        this == null -> default
        fileName == null -> default
        else -> fileName.toString()?.substringAfterLast('.', default)
    }.toLowerCase()
}

inline fun File?.extension(default: String = ""): String {
    return when {
        this == null -> default
        else -> path.substringAfterLast('.', default)
    }.toLowerCase()
}