package com.github.nayasis.sample.forms

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
    val name     = bind{ item.getProperty(Customer::name) }
    val birthday = bind{ item.getProperty(Customer::birthday) }
    val street   = bind{ item.getProperty(Customer::street) }
    val zip      = bind{ item.getProperty(Customer::zip) }
    val city     = bind{ item.getProperty(Customer::city) }
}