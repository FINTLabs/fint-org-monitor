package no.fint.orgmonitor.sync

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

/**
 * Persists the last-seen update timestamp per organization, keyed by orgId so that
 * several namespaces can share the same database without colliding.
 *
 */
@Document(collection = "sync_state")
data class SyncState(
    @Id
    val orgId: String = "",
    val lastUpdated: Long = 0L,
)
