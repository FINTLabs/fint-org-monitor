package no.fint.orgmonitor

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

// TODO: rename?
@Component
@ConfigurationProperties(prefix = "fint.orgmonitor")
data class Config(
    var endpoint: String = "",
    var orgid: String = "",
    var sender: String = "",
    var smtpUsername: String = "",
    var smtpPassword: String = "",
    var smtpServer: String = "",
    var smtpPort: String = "",
    var recipients: List<String> = emptyList(),
)
