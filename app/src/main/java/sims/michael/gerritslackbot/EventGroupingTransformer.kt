package sims.michael.gerritslackbot

import io.reactivex.Flowable
import io.reactivex.FlowableTransformer
import io.reactivex.rxkotlin.toFlowable
import org.reactivestreams.Publisher
import sims.michael.gerritslackbot.model.*

/**
 * Flowable transformer for [ChangeEvent] lists that uses a list of [ChangeMatcher] to group changes into instances
 * of [EventGroup].
 */
class EventGroupingTransformer(
        private val changeMatchers: List<ChangeMatcher>
) : FlowableTransformer<List<ChangeEvent>, EventGroup<*>> {

    override fun apply(upstream: Flowable<List<ChangeEvent>>): Publisher<EventGroup<*>> = upstream.flatMap { events ->

        /* Reviewers Added - filters are not applied (messages will be addressed via DM if the reviewer is recognized) */
        val reviewerAddedEvents = events.filterIsInstance(ReviewerAddedEvent::class.java)
        val reviewerAddedMessages = reviewerAddedEvents.groupBy { event ->
            EventGroup(
                    ReviewerAddedEvent::class.java,
                    event.change.project,
                    event.change.branch,
                    event.reviewer.username
            )
        }.mapNotNull { (group, events) ->
            group.copy(events = events)
        }

        // New Changes
        val patchSetCreatedEvents = events.filterIsInstance(PatchSetCreatedEvent::class.java)
        val newChanges = patchSetCreatedEvents.groupBy { event ->
            changeMatchers.firstOrNull { it.matches(event) }?.let { matcher ->
                if (matcher.channel != null) {
                    EventGroup(
                            PatchSetCreatedEvent::class.java,
                            event.change.project,
                            event.change.branch,
                            event.change.owner.username,
                            matcher.channel
                    )
                } else null
            }
        }.filterNotNullKeys().map { (group, events) -> group.copy(events = events) }

        // New Comments
        val commentAddedEvents = events.filterIsInstance(CommentAddedEvent::class.java)
        val newComments = commentAddedEvents.groupBy { event ->
            changeMatchers.firstOrNull { it.matches(event) }?.let { matcher ->
                if (matcher.channel != null) {
                    EventGroup(
                            CommentAddedEvent::class.java,
                            event.change.project,
                            event.change.branch,
                            event.change.owner.username,
                            matcher.channel
                    )
                } else null
            }
        }.filterNotNullKeys().map { (group, events) -> group.copy(events = events) }

        // Merged Changes
        val changeMergedEvents = events.filterIsInstance(ChangeMergedEvent::class.java)
        val mergedChanges = changeMergedEvents.groupBy { event ->
            changeMatchers.firstOrNull { it.matches(event) }?.let { matcher ->
                if (matcher.channel != null) {
                    EventGroup(
                            ChangeMergedEvent::class.java,
                            event.change.project,
                            event.change.branch,
                            event.change.owner.username,
                            matcher.channel
                    )
                } else null
            }
        }.filterNotNullKeys().map { (group, events) -> group.copy(events = events) }

        (reviewerAddedMessages + newChanges + newComments + mergedChanges).toFlowable()
    }

    private fun <K : Any?, V> Map<K?, V>.filterNotNullKeys(): Map<K, V> {
        val destination: MutableMap<K, V> = LinkedHashMap<K, V>()
        for ((key, value) in this) {
            if (key != null) destination[key] = value
        }
        return destination
    }
}
