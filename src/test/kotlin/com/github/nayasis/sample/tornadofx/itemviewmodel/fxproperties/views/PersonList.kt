package com.github.nayasis.sample.tornadofx.itemviewmodel.fxproperties.views

import com.github.nayasis.sample.tornadofx.itemviewmodel.fxproperties.controller.PersonController
import com.github.nayasis.sample.tornadofx.itemviewmodel.fxproperties.model.Person
import tornadofx.*

class PersonList : View() {
    val controller: PersonController by inject()

    override val root = tableview(controller.persons) {
        column("Id", Person::idProperty)
        column("Name", Person::nameProperty)
        bindSelected(controller.selectedPerson)
        smartResize()
    }
}
