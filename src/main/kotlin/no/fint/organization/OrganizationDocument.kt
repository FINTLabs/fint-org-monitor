package no.fint.organization

import com.vladmihalcea.hibernate.type.json.JsonType
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import no.fint.model.administrasjon.organisasjon.Organisasjonselement
import org.hibernate.annotations.Type
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

@Entity
@Table(name = "organization_document")
data class OrganizationDocument(
    @Id
    @Column(nullable = false, updatable = false)
    var id: String = "",
    @Column
    var orgId: String = "",
    @Type(JsonType::class)
    @Column(name = "organisasjonselement", columnDefinition = "jsonb")
    var data: Organisasjonselement? = null,
    @Column
    var overordnet: String? = null,
    @ElementCollection
    @CollectionTable(name = "organization_underordnet", joinColumns = [JoinColumn(name = "organization_id")])
    @Column(name = "underordnet")
    var underordnet: List<String>? = null,
    @LastModifiedDate
    @Column
    var lastModifiedDate: LocalDateTime? = null,
) {
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
