package no.fint

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OrgMonitorApplication

fun main(args: Array<String>) {
    runApplication<OrgMonitorApplication>(*args)
}
