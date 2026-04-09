package no.fint

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.config.EnableMongoAuditing

@SpringBootApplication
@EnableMongoAuditing
class OrgMonitorApplication

fun main(args: Array<String>) {
    runApplication<OrgMonitorApplication>(*args)
}
