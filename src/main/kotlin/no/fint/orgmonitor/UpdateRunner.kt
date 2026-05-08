package no.fint.orgmonitor

import no.fint.orgmonitor.organization.OrganizationService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/*
 * This class is needed to run as a FlaisJob
 */
@Component
@ConditionalOnProperty(value = ["fint.sync.enabled"], havingValue = "true", matchIfMissing = true)
class UpdateRunner : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var organizationService: OrganizationService

    override fun run(args: ApplicationArguments) {
        logger.info("Running Organization update check")
        organizationService.update()
    }
}
