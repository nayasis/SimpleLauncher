package com.github.nayasis.sample.tornadofx.forms

import javafx.beans.property.Property
import tornadofx.ItemViewModel
import tornadofx.getProperty
import tornadofx.property
import java.time.LocalDate

class Customer {

    var name     by property<String>()
    var birthday by property<LocalDate>()
    var street   by property<String>()
    var zip      by property<String>()
    var city     by property<String>()

    override fun toString(): String = name

}

class CustomerModel: ItemViewModel<Customer>(Customer()) {
    val name: Property<String>        = bind{ item.getProperty(Customer::name) }
    val birthday: Property<LocalDate> = bind{ item.getProperty(Customer::birthday) }
    val street: Property<String>      = bind{ item.getProperty(Customer::street) }
    val zip: Property<String>         = bind{ item.getProperty(Customer::zip) }
    val city: Property<String>        = bind{ item.getProperty(Customer::city) }
}