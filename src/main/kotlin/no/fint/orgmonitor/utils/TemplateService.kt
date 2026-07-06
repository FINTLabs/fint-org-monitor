package no.fint.orgmonitor.utils

import no.fint.orgmonitor.organization.OrganizationDocument
import no.fint.orgmonitor.organization.SimpleOrganizationInfo
import no.fint.orgmonitor.organization.UpdatedOrganizationReport
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
        organizationTree: String,
    ): String =
        templateEngine.process(
            "email-template",
            Context().apply {
                setVariable("added", added)
                setVariable("updated", updated.map { UpdatedOrganizationReport.from(it.first, it.second) })
                setVariable("parentInfo", parentInfo)
                setVariable("organizationTree", organizationTree)
                setVariable("emptyChangedFields", linkedSetOf<String>())
                setVariable("highlightStyle", HIGHLIGHT_STYLE)
            },
        )

    private companion object {
        const val HIGHLIGHT_STYLE = "background-color: #fff59d"
    }
}
