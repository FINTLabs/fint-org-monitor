package no.fint.orgmonitor.sync

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface SyncStateRepository : MongoRepository<SyncState, String>
