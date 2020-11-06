package com.github.nayasis.simplelauncher

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SimplelauncherApplication

fun main(args: Array<String>) {
	runApplication<SimplelauncherApplication>(*args)
}
