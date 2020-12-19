package com.github.nayasis.kotlin.basica.exception

import java.lang.Integer.max
import java.lang.Integer.min

class Caller {

    val className: String
    val fileName: String
    val methodName: String
    val lineNo: Int

    val stacktrace = Thread.currentThread().stackTrace

    constructor(depth: Int) {
        val idx = min( max(depth,0), stacktrace.size )
        stacktrace[idx].let {
            className  = it.className
            fileName   = it.fileName
            methodName = it.methodName
            lineNo     = it.lineNumber
        }
    }

    fun stacktraceToString(): String {
        return ArrayList<String>().apply {
            stacktrace.map { this.add(it.toString()) }
        }.joinToString("\n")
    }

}