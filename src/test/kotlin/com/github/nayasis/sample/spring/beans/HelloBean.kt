package com.github.nayasis.sample.spring.beans

import org.springframework.stereotype.Component

@Component
class HelloBean {
    fun helloworld(): String = "Hello by di()"
}