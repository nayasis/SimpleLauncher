package com.github.nayasis.sample.tornadofx.login

import tornadofx.Stylesheet
import tornadofx.box
import tornadofx.cssclass
import tornadofx.pt
import tornadofx.px

class Styles: Stylesheet() {
    companion object {
        val loginScreen by cssclass()
    }
    init {
        root {
            fontSize = 9.pt
            fontFamily = "Arial"
        }
        loginScreen {
            padding = box(15.px)
            vgap = 7.px
            hgap = 10.px
        }
    }
}