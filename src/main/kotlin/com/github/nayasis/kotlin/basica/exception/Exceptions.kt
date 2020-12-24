package com.github.nayasis.kotlin.basica.exception

open class NotFound : Exception {
    constructor(): super()
    constructor(message: String?): super(message)
    constructor(message: String?, cause: Throwable?): super(message, cause)
    constructor(cause: Throwable?): super(cause)
}