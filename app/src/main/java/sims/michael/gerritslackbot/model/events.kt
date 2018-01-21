package sims.michael.gerritslackbot.model

import com.fasterxml.jackson.annotation.*

/*
 * Schema originally sourced from Documentation/cmd-stream-events.html file in gerrit WAR file, as well as the source
 * code for gerrit.
 *
 * When updating this, it's probably best to do it from the documentation that comes with the exact version you're
 * using, in case the schema changes.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type",
        visible = true, defaultImpl = UnknownEvent::class
)
@JsonSubTypes(
        JsonSubTypes.Type(value = AssigneeChangedEvent::class),
        JsonSubTypes.Type(value = HashtagsChangedEvent::class),
        JsonSubTypes.Type(value = ChangeAbandonedEvent::class),
        JsonSubTypes.Type(value = ChangeMergedEvent::class),
        JsonSubTypes.Type(value = ChangeRestoredEvent::class),
        JsonSubTypes.Type(value = CommentAddedEvent::class),
        JsonSubTypes.Type(value = DraftPublishedEvent::class),
        JsonSubTypes.Type(value = PatchSetCreatedEvent::class),
        JsonSubTypes.Type(value = ReviewerAddedEvent::class),
        JsonSubTypes.Type(value = ReviewerDeletedEvent::class),
        JsonSubTypes.Type(value = VoteDeletedEvent::class),
        JsonSubTypes.Type(value = TopicChangedEvent::class),
        JsonSubTypes.Type(value = DroppedOutputEvent::class),
        JsonSubTypes.Type(value = MergeFailedEvent::class),
        JsonSubTypes.Type(value = RefUpdatedEvent::class)
)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed class Event {
    abstract val type: String
    abstract val eventCreatedOn: Long
}

sealed class ChangeEvent : Event() {
    abstract val change: ChangeAttribute
}

/** Sent when the assignee of a change has been modified. */
@JsonTypeName("assignee-changed")
data class AssigneeChangedEvent(
        val changer: AccountAttribute?,
        val oldAssignee: AccountAttribute?,
        override val change: ChangeAttribute,
        override val type: String,
        override val eventCreatedOn: Long
) : ChangeEvent()

@JsonTypeName("hashtags-changed")
data class HashtagsChangedEvent(
        val editor: AccountAttribute?,
        val added: List<String>?,
        val removed: List<String>?,
        val hashtags: List<String>?,
        override val change: ChangeAttribute,
        override val type: String,
        override val eventCreatedOn: Long
) : ChangeEvent()

sealed class PatchSetEvent : ChangeEvent() {
    abstract val patchSet: PatchSetAttribute
}

@JsonTypeName("change-abandoned")
data class ChangeAbandonedEvent(
        val abandoner: AccountAttribute,
        val reason: String?,
        override val patchSet: PatchSetAttribute,
        override val change: ChangeAttribute,
        override val type: String,
        override val eventCreatedOn: Long
) : PatchSetEvent()

@JsonTypeName("change-merged")
data class ChangeMergedEvent(
        val submitter: AccountAttribute,
        val newRev: String,
        override val patchSet: PatchSetAttribute,
        override val change: ChangeAttribute,
        override val type: String,
        override val eventCreatedOn: Long
) : PatchSetEvent()

@JsonTypeName("change-restored")
data class ChangeRestoredEvent(
        val restorer: AccountAttribute?,
        val reason: String?,
        override val patchSet: PatchSetAttribute,
        override val change: ChangeAttribute,
        override val type: String,
        override val eventCreatedOn: Long
) : PatchSetEvent()

@JsonTypeName("comment-added")
data class CommentAddedEvent(
        val author: AccountAttribute,
        val approvals: List<ApprovalAttribute>?,
        val comment: String,
        override val patchSet: PatchSetAttribute,
        override val change: ChangeAttribute,
        override val type: String,
        override val eventCreatedOn: Long
) : PatchSetEvent()

@JsonTypeName("draft-published")
data class DraftPublishedEvent(
        val uploader: AccountAttribute?,
        override val patchSet: PatchSetAttribute,
        override val change: ChangeAttribute,
        override val type: String,
        override val eventCreatedOn: Long
) : PatchSetEvent()

@JsonTypeName("patchset-created")
data class PatchSetCreatedEvent(
        val uploader: AccountAttribute,
        override val patchSet: PatchSetAttribute,
        override val change: ChangeAttribute,
        override val type: String,
        override val eventCreatedOn: Long

) : PatchSetEvent()

@JsonTypeName("reviewer-added")
data class ReviewerAddedEvent(
        val reviewer: AccountAttribute,
        override val patchSet: PatchSetAttribute,
        override val change: ChangeAttribute,
        override val type: String,
        override val eventCreatedOn: Long
) : PatchSetEvent()

@JsonTypeName("reviewer-deleted")
data class ReviewerDeletedEvent(
        val reviewer: AccountAttribute?,
        val remover: AccountAttribute?,
        val approvals: List<ApprovalAttribute>?,
        val comment: String?,
        override val patchSet: PatchSetAttribute,
        override val change: ChangeAttribute,
        override val type: String,
        override val eventCreatedOn: Long
) : PatchSetEvent()

@JsonTypeName("vote-deleted")
data class VoteDeletedEvent(
        val reviewer: AccountAttribute?,
        val remover: AccountAttribute?,
        val approvals: List<ApprovalAttribute>?,
        val comment: String?,
        override val patchSet: PatchSetAttribute,
        override val change: ChangeAttribute,
        override val type: String,
        override val eventCreatedOn: Long
) : PatchSetEvent()

@JsonTypeName("topic-changed")
data class TopicChangedEvent(
        val changer: AccountAttribute?,
        val oldTopic: String?,
        override val change: ChangeAttribute,
        override val type: String,
        override val eventCreatedOn: Long
) : ChangeEvent()

@JsonTypeName("dropped-output")
data class DroppedOutputEvent(override val type: String, override val eventCreatedOn: Long) : Event()

@JsonTypeName("merge-failed")
data class MergeFailedEvent(override val type: String, override val eventCreatedOn: Long) : Event()

@JsonTypeName("ref-updated")
data class RefUpdatedEvent(
        val submitter: AccountAttribute,
        val refUpdate: RefUpdateAttribute,
        override val type: String,
        override val eventCreatedOn: Long
) : Event()

data class UnknownEvent(
        override val type: String,
        override val eventCreatedOn: Long,
        @get:JsonAnyGetter val nodes: MutableMap<String, Any?> = mutableMapOf()
) : Event() {

    @JsonAnySetter operator fun set(key: String, value: Any?) = nodes.put(key, value)
}
