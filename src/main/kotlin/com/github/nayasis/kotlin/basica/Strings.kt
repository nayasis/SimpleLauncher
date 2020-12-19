package com.github.nayasis.kotlin.basica

import com.github.nayasis.basica.model.Messages

fun String.message(): String = Messages.get(this)