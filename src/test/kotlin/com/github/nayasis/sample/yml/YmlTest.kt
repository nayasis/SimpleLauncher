package com.github.nayasis.sample.yml

import com.github.nayasis.kotlin.basica.core.string.toResource
import com.github.nayasis.kotlin.basica.core.url.toInputStream
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml

class YmlTest {

    @Test
    fun basic() {
//        val map: HashMap<String,Any> = Yaml().load("application.yml".toResource()!!.toInputStream())
        val map: HashMap<String,Any> = Yaml().load("test.yml".toResource()!!.toInputStream())
//        val map = Yaml().load<Any>("3.0: 2018-07-22")
        println(map)
    }

    @Test
    fun multiple() {
        val maps: MutableIterable<Any> = Yaml().loadAll("application.yml".toResource()!!.toInputStream())
        maps.forEach { map ->
            println(">> ${map.javaClass}\n\t$map")
        }
    }

}