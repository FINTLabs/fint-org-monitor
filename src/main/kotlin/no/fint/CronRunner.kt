import no.fint.organization.OrganizationService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/*
 * This class is needed to run as a cronjob the old way. Should be deleted when FlaisJob is implemented.
 */
@Service
class CronRunner(
    private val organizationService: OrganizationService,
) {
    @Scheduled(cron = "\${fint.sync.cron}")
    fun update() {
        organizationService.update()
    }
}
