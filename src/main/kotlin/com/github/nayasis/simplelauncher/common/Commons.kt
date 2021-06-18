package com.github.nayasis.simplelauncher.common

fun makeKeyword(vararg word: String?): Set<String> {
    return word.toList().filterNotNull().joinToString(" ")
        .split("[ \t\n:,.|;]".toRegex())
        .map { it.toLowerCase() }
        .toSet()
}