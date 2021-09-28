package com.github.nayasis.sample.itemviewmodel.fxproperties.views

import tornadofx.*


class ItemViewModelWithFxMainView : View("Person Editor") {
    override val root = hbox {
        add<PersonList>()
        add<PersonEditor>()
    }
}
