package no.fint.organization

import no.novari.fint.model.administrasjon.organisasjon.Organisasjonselement
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "organization_document")
data class OrganizationDocument(
    @Id
    var id: String = "",
    var orgId: String = "",
    var data: Organisasjonselement? = null,
    var overordnet: String? = null,
    var underordnet: List<String>? = null,
    @LastModifiedDate
    var lastModifiedDate: LocalDateTime? = null,
) {
    /*
     * The equals is overriden so that we can ignore the order of the underordnet list.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OrganizationDocument

        if (orgId != other.orgId) return false
        if (data != other.data) return false
        if (overordnet != other.overordnet) return false

        // We don't care about the order of the list. Therefore, we sort it first before comparison.
        val thisUnderordnet = underordnet?.sorted()
        val otherUnderordnet = other.underordnet?.sorted()

        return thisUnderordnet == otherUnderordnet
    }

    override fun hashCode(): Int {
        var result = orgId.hashCode()
        result = 31 * result + (data?.hashCode() ?: 0)
        result = 31 * result + (overordnet?.hashCode() ?: 0)
        result = 31 * result + (underordnet?.toSet()?.hashCode() ?: 0)
        return result
    }
}
