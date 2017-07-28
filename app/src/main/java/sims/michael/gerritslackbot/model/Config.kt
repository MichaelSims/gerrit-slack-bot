package sims.michael.gerritslackbot.model

import com.fasterxml.jackson.annotation.JsonProperty
import sims.michael.gerritslackbot.SSHStreamPublisher
import sims.michael.gerritslackbot.slack.EventGroupToSlackMessageTransformer

data class Config(
        @JsonProperty("Event matchers") val changeMatchers: List<ChangeMatcher>,
        @JsonProperty("Gerrit to Slack username mappings") val gerritToSlackMap: Map<String, String> = emptyMap(),
        @JsonProperty("Gerrit stream configuration") val gerritStreamConfig: SSHStreamPublisher.Config,
        @JsonProperty("Slack incoming webhook URL") val slackApiUrl: String,
        @JsonProperty("Slack message configuration") val slackMessageConfig: EventGroupToSlackMessageTransformer.Config,
        @JsonProperty("HTTP client configuration") val httpConfig: HttpConfig = HttpConfig(),
        @JsonProperty("Event buffer timeout in seconds") val eventBufferTimeoutInSeconds: Long = 15
) {
    data class HttpConfig(
            val trustAllSSLCerts: Boolean = false,
            val proxyHost: String? = "",
            val proxyPort: Int = 8080
    )
}
