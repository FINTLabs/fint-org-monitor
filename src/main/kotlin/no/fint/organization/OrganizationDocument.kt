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
    fun overordnetId(): String? = overordnet?.substringAfterLast("/")
//    @field:Transient
//    val overordnetId: String?
//        get() = overordnet?.substringAfterLast("/")
//
//    @field:Transient
//    val underordnetId: List<String>
//        get() = underordnet?.map { it.substringAfterLast("/") } ?: emptyList()
}
