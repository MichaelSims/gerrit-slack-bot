package sims.michael.gerritslackbot.slack

import io.reactivex.Flowable
import io.reactivex.FlowableTransformer
import okhttp3.HttpUrl
import org.apache.commons.lang3.StringEscapeUtils
import org.reactivestreams.Publisher
import sims.michael.gerritslackbot.SlackNameResolver
import sims.michael.gerritslackbot.model.*
import java.net.URLEncoder

class EventGroupToSlackMessageTransformer(
        private val slackNameResolver: SlackNameResolver,
        private val config: Config
) : FlowableTransformer<EventGroup<*>, SlackMessage> {

    data class Config(
            val username: String,
            val iconUrl: String? = null,
            val directMessagesEnabled: Boolean = false
    )

    override fun apply(upstream: Flowable<EventGroup<*>>): Publisher<SlackMessage> = upstream.flatMap { eventGroup ->

        @Suppress("UNCHECKED_CAST")
        val slackMessage: SlackMessage? = when (eventGroup.type) {
            ReviewerAddedEvent::class.java -> {
                if (config.directMessagesEnabled) {
                    (eventGroup as EventGroup<ReviewerAddedEvent>).toReviewerAddedMessage()
                } else null
            }
            PatchSetCreatedEvent::class.java -> (eventGroup as EventGroup<PatchSetCreatedEvent>).toNewChangesMessage()
            CommentAddedEvent::class.java -> (eventGroup as EventGroup<CommentAddedEvent>).toNewCommentsMessage()
            ChangeMergedEvent::class.java -> (eventGroup as EventGroup<ChangeMergedEvent>).toMergedChangesMessage()
            else -> null
        }

        if (slackMessage != null) Flowable.just(slackMessage) else Flowable.empty()
    }

    /** Returns a "reviewer added" slack message via DM is the username is recognized, null otherwise */
    private fun EventGroup<ReviewerAddedEvent>.toReviewerAddedMessage(): SlackMessage? {
        val slackName = slackNameResolver[username] ?: return null
        val heading = "Hi there! You have been added as a reviewer to the following ${commitOrCommits(events.size)} " +
                "in ${toProjectBranchPrefix()}:"
        return SlackMessage(
                username = config.username,
                icon_url = config.iconUrl,
                text = heading,
                channel = slackName,
                // or better yet, make this disabled by default until you take steps to enable it
                attachments = listOf(Attachment(
                        fallback = "Summary of changes",
                        text = events.joinToString("\n") { event -> event.toSlackSummary() }
                ))
        )
    }

    private fun EventGroup<PatchSetCreatedEvent>.toNewChangesMessage(): SlackMessage {
        val heading = "${toProjectBranchPrefix()} " +
                "${events.size} new ${changeOrChanges(events.size)} " +
                "by ${username.resolveSlackName()}:"

        return SlackMessage(
                username = config.username,
                icon_url = config.iconUrl,
                channel = channel,
                text = heading,
                attachments = listOf(Attachment(
                        fallback = "Summary of changes",
                        text = events.map { it.toSlackSummary() }.joinToString("\n")
                ))
        )
    }

    private fun EventGroup<CommentAddedEvent>.toNewCommentsMessage(): SlackMessage {
        val heading = "${toProjectBranchPrefix()} " +
                "${username.resolveSlackName()} has added " +
                "${events.size} new ${commentOrComments(events.size)}:"

        return SlackMessage(
                username = config.username,
                icon_url = config.iconUrl,
                channel = channel,
                text = heading,
                attachments = listOf(Attachment(
                        fallback = "Summary of comments",
                        text = events.flatMap { event ->
                            mutableListOf<String>().apply {
                                val codeReview = event.approvals.orEmpty().find { it.type == "Code-Review" }
                                val codeReviewVote = codeReview?.value?.toIntOrNull()
                                if (codeReviewVote != null) {
                                    add("*${codeReviewVote.voteToString()}* ${event.toSlackSummary()}")
                                }

                                val verification = event.approvals.orEmpty().find { it.type == "Verified" }
                                val verificationVote = verification?.value?.toIntOrNull()
                                if (verificationVote != null) {
                                    add("*${verificationVote.verifyVoteToString()}* ${event.toSlackSummary()}")
                                }

                                if (!event.toSlackShortComment().isNullOrBlank()) {
                                    add("Commented on ${event.toSlackSummary()}: " +
                                            "\"${event.toSlackShortComment()}\"")
                                }
                            }
                        }.joinToString("\n"),
                        mrkdwn_in = listOf("text")
                ))
        )
    }

    private fun EventGroup<ChangeMergedEvent>.toMergedChangesMessage(): SlackMessage {
        val heading = "${toProjectBranchPrefix()} ${username.resolveSlackName()} has merged ${events.size} " +
                "${changeOrChanges(events.size)}:"

        return SlackMessage(
                username = config.username,
                icon_url = config.iconUrl,
                channel = channel,
                text = heading,
                attachments = listOf(Attachment(
                        fallback = "Summary of changes",
                        text = events.map { it.toSlackSummary() }.joinToString("\n")
                ))
        )
    }

    private fun EventGroup<*>.toProjectBranchPrefix(): String {
        val baseUrl = HttpUrl.parse(events.first().change.url).let { "${it.scheme()}://${it.host()}" }
        val projectBranchLink = "$baseUrl/#/q/project:${project.escapeUrlParameter()}" +
                "+branch:${branch.escapeUrlParameter()}+status:open"
        return "<$projectBranchLink|[$project:$branch]>"
    }

    private fun String.resolveSlackName() = slackNameResolver[this].let { slackName ->
        if (slackName != null) "<$slackName> ($this)" else this
    }

    private fun Int.voteToString(): String = (if (this > 0) "+$this" else this.toString()) + "'d"
    private fun Int.verifyVoteToString(): String = if (this > 0) "Verified (+1)" else "Failed to verify (-1)"

    private fun changeOrChanges(size: Int): String = if (size == 1) "change" else "changes"
    private fun commentOrComments(size: Int): String = if (size == 1) "comment" else "comments"
    private fun commitOrCommits(size: Int): String = if (size == 1) "commit" else "commits"
    private fun String.escapeHtml(): String? = StringEscapeUtils.escapeHtml4(this)
    private fun String.escapeUrlParameter(): String? = URLEncoder.encode(this, "UTF-8")
    private fun PatchSetEvent.toSlackSummary(): String {
        return "<${change.url}|${change.subject?.escapeHtml()} (patch ${patchSet.number})>"
    }

    private fun CommentAddedEvent.toSlackShortComment(): String? {
        val delimiter = "\n\n"
        return comment.split(delimiter)
                .filterNot { it.contains("Patch Set \\d+".toRegex()) }
                .joinToString(delimiter).escapeHtml()
    }

    private fun <K : Any?, V> Map<K?, V>.filterNotNullKeys(): Map<K, V> {
        val destination: MutableMap<K, V> = LinkedHashMap<K, V>()
        for ((key, value) in this) {
            if (key != null) destination[key] = value
        }
        return destination
    }
}
