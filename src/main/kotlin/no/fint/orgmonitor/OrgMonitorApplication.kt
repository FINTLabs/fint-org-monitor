package no.fint.orgmonitor

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableMongoAuditing
@EnableScheduling
class OrgMonitorApplication

fun main(args: Array<String>) {
    runApplication<OrgMonitorApplication>(*args)
}
