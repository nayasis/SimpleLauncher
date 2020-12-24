package com.github.nayasis.kotlin.basica

import com.github.nayasis.basica.model.Messages
import java.util.*

fun String.message(locale: Locale? = null): String = Messages.get(locale, this)