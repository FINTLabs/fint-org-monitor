package no.fint.orgmonitor.organization

data class UpdatedOrganizationReport(
    val before: OrganizationDocument,
    val after: OrganizationDocument,
    val changedFields: Set<OrganizationField>,
) {
    val changedFieldNames: Set<String> = changedFields.mapTo(linkedSetOf()) { it.name }

    companion object {
        fun from(
            before: OrganizationDocument,
            after: OrganizationDocument,
        ): UpdatedOrganizationReport =
            UpdatedOrganizationReport(
                before = before,
                after = after,
                changedFields = OrganizationField.changedFields(before, after),
            )
    }
}
