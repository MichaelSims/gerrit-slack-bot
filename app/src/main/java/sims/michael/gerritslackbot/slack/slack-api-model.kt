package sims.michael.gerritslackbot.slack

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SlackMessage(
        val text: String,
        val username: String? = null,
        val icon_url: String? = null,
        val icon_emoji: String? = null,
        val channel: String? = null,
        val attachments: List<Attachment> = emptyList()
)

data class Attachment(
        val fallback: String,
        val text: String? = null,
        val pretext: String? = null,
        val color: String? = null,
        val fields: List<Field> = emptyList(),
        // Valid values are ["pretext", "text", "fields"]
        // (See https://api.slack.com/docs/message-formatting#message_formatting)
        val mrkdwn_in: List<String> = emptyList()
)

data class Field(val title: String, val value: String, val short: Boolean = false)
