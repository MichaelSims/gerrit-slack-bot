package sims.michael.gerritslackbot.model

/**
 * A list of [ChangeEvent]s grouped by project, branch, username, and slack channel. Each group corresponds with
 * a single Slack message.
 */
data class EventGroup<T : ChangeEvent>(
        val type: Class<T>,
        val project: String,
        val branch: String,
        val username: String,
        val channel: String? = null,
        val events: List<T> = emptyList()
)
