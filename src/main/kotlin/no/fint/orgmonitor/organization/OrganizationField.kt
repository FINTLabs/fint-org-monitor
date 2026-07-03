package no.fint.orgmonitor.organization

enum class OrganizationField {
    KORTNAVN,
    NAVN,
    KODE,
    ID,
    ORGANISASJONSNUMMER,
    OVERORDNET,
    UNDERORDNET,
    GYLDIGHETSPERIODE_START,
    GYLDIGHETSPERIODE_SLUTT,
    ;

    companion object {
        fun changedFields(
            before: OrganizationDocument,
            after: OrganizationDocument,
        ): Set<OrganizationField> =
            buildSet {
                if (before.data?.kortnavn != after.data?.kortnavn) add(KORTNAVN)
                if (before.data?.navn != after.data?.navn) add(NAVN)
                if (before.data?.organisasjonsKode?.identifikatorverdi != after.data?.organisasjonsKode?.identifikatorverdi) {
                    add(KODE)
                }
                if (before.data?.organisasjonsId?.identifikatorverdi != after.data?.organisasjonsId?.identifikatorverdi) add(ID)
                if (before.data?.organisasjonsnummer?.identifikatorverdi != after.data?.organisasjonsnummer?.identifikatorverdi) {
                    add(ORGANISASJONSNUMMER)
                }
                if (before.overordnet != after.overordnet) add(OVERORDNET)
                if (before.underordnet?.sorted() != after.underordnet?.sorted()) add(UNDERORDNET)
                if (before.data?.gyldighetsperiode?.start != after.data?.gyldighetsperiode?.start) {
                    add(GYLDIGHETSPERIODE_START)
                }
                if (before.data?.gyldighetsperiode?.slutt != after.data?.gyldighetsperiode?.slutt) {
                    add(GYLDIGHETSPERIODE_SLUTT)
                }
            }
    }
}
