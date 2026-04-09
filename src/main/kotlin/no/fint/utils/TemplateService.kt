package no.fint.utils

import no.fint.organization.OrganizationDocument
import no.fint.organization.SimpleOrganizationInfo
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context

@Service
class TemplateService(
    private val templateEngine: TemplateEngine,
) {
    fun render(
        added: List<OrganizationDocument>,
        updated: List<Pair<OrganizationDocument, OrganizationDocument>>,
        parentInfo: List<SimpleOrganizationInfo>,
    ): String =
        templateEngine.process(
            "email-template",
            Context().apply {
                setVariable("added", added)
                setVariable("updated", updated)
                setVariable("parentInfo", parentInfo)
            },
        )
}
