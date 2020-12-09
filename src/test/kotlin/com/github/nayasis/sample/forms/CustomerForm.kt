package com.github.nayasis.sample.forms

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.HOME
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.USER
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import tornadofx.*
import javax.management.NotificationBroadcasterSupport

class CustomerForm: View("Register Customer") {

    val model: CustomerModel by inject()

    override val root = form {
        fieldset("Personal Information", FontAwesomeIconView(USER)) {
            field("Name") {
                textfield(model.name).required()
            }
            field("Birthday") {
                datepicker(model.birthday)
            }
        }
        fieldset("Address",FontAwesomeIconView(HOME)) {
            field("Street") {
                textfield(model.street).required()
            }
            field("Zip / City") {
                textfield(model.zip) {
                    addClass(Styles.zip)
                    required()
                }
                textfield(model.city).required()
            }
        }
        button("Save") {
            action {
                val customer = model.item
                Notifications.create()
            }
        }
    }
}