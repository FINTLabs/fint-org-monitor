package no.fint.orgmonitor

import no.fint.orgmonitor.organization.OrganizationService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/*
 * This class is needed to run as a cronjob the old way. Should be deleted when FlaisJob is implemented.
 */
@Service
class CronRunner(
    private val organizationService: OrganizationService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${fint.orgmonitor.cron}")
    fun update() {
        logger.debug("Starting update")
        organizationService.update()
    }
}
