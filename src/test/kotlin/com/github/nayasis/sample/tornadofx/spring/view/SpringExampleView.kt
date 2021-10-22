package com.github.nayasis.sample.tornadofx.spring.view

import com.github.nayasis.sample.spring.beans.HelloBean
import tornadofx.*

class SpringExampleView: View("Example View") {

    val bean: HelloBean by di()

    override val root = vbox{
        label(bean.helloworld()).paddingAll = 20
    }

}