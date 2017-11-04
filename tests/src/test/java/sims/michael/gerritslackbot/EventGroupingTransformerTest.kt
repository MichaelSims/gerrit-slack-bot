package sims.michael.gerritslackbot

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.toFlowable
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import sims.michael.gerritslackbot.container.ObjectMappers
import sims.michael.gerritslackbot.container.defaultModule
import sims.michael.gerritslackbot.model.*

class EventGroupingTransformerTest {

    private lateinit var objectMapper: ObjectMapper

    private lateinit var stringToEvent: StringToEventTransformer

    @Before
    fun setUp() {
        val container = Kodein {
            import(defaultModule)
        }
        stringToEvent = container.instance()
        objectMapper = container.instance(ObjectMappers.JSON)
    }

    @Test
    fun can_exclude_events_based_on_subject() {
        val eventMatchingTransformer = EventGroupingTransformer(listOf(
                ChangeMatcher("*", "*", "^WIP: ", null),
                ChangeMatcher("*", "*", "*", "channel")
        ))
        val groups = getEventGroupsWithTransformer(eventMatchingTransformer)
        assertFalse(groups.any { it.events.any { it.change.id == "0" } })
    }

    @Test
    fun can_match_events_based_on_project() {
        val eventMatchingTransformer = EventGroupingTransformer(listOf(
                ChangeMatcher("Froboztic", "*", "*", "otherChannel"),
                ChangeMatcher("*", "*", "*", "channel")
        ))
        val groups = getEventGroupsWithTransformer(eventMatchingTransformer)
        assertNotNull(groups.firstOrNull { it.project == "Froboztic" })
        assertEquals("otherChannel", groups.first { it.project == "Froboztic" }.channel)
    }

    @Test
    fun can_match_events_based_on_branch() {
        val eventMatchingTransformer = EventGroupingTransformer(listOf(
                ChangeMatcher("*", "feature-two", "*", "otherChannel"),
                ChangeMatcher("*", "*", "*", "channel")
        ))
        val groups = getEventGroupsWithTransformer(eventMatchingTransformer)
        assertNotNull(groups.firstOrNull { it.branch == "feature-two" })
        assertEquals("otherChannel", groups.first { it.branch == "feature-two" }.channel)
    }

    @Test
    fun can_match_events_that_are_only_verifications() {
        val eventMatchingTransformer = EventGroupingTransformer(listOf(
                ChangeMatcher("*", "*", "*", channel = null, isVerificationOnly = true),
                ChangeMatcher("*", "*", "*", "channel")
        ))
        val groups = getEventGroupsWithTransformer(eventMatchingTransformer)
        assertFalse("Change with only a verification vote was matched",
                groups.any { it.events.any { it.change.id == "2" } })
        assertTrue("Change with both a verification vote and a code review vote was not matched",
                groups.any { it.events.any { it.change.id == "3" } })
    }

    @Test
    fun can_match_events_based_on_change_kind() {
        val eventMatchingTransformer = EventGroupingTransformer(listOf(
                ChangeMatcher("*", "*", "*", "channel", changeKind = "rework")
        ))
        val groups = getEventGroupsWithTransformer(eventMatchingTransformer)
        assertFalse("Some of the changes are not reworks",
                groups.any { it.events.any { it is PatchSetCreatedEvent && it.patchSet.kind != ChangeKind.REWORK } })
    }

    @Test
    fun change_kind_matches_for_patch_set_created_events_only() {
        val eventMatchingTransformer = EventGroupingTransformer(listOf(
                ChangeMatcher("*", "*", "*", "channel", changeKind = "rework")
        ))
        val groups = getEventGroupsWithTransformer(eventMatchingTransformer)
        assertTrue("Change kind matcher should be applied to PatchSetCreatedEvent only",
                groups.any { it.events.any { it.change.id == "6" } })
    }

    @Test
    fun comment_added_events_are_grouped_by_change_owner() {
        val eventMatchingTransformer = EventGroupingTransformer(listOf(
                ChangeMatcher("*", "*", "*", "channel")
        ))
        val groups = getEventGroupsWithTransformer(eventMatchingTransformer)
        val fry = "philipjfry"

        // For this test to be useful the test data needs to have more than one comment added event associated with
        // a change owned by the same user.
        val commentAddedEvents = groups.flatMap { it.events }.filterIsInstance<CommentAddedEvent>()
        assertTrue("Fry needs more than one comment on his commits for this test to be valid",
                commentAddedEvents.groupBy { it.change.owner }.any { it.key.username == fry && it.value.size > 1 })

        assertTrue("There should be a group of comment added events from different authors on commits owned by Fry",
                groups.any {
                    it.type == CommentAddedEvent::class.java
                            && it.username == fry
                            && it.events.all { it.change.owner.username == fry }
                })
    }

    @Test
    fun change_merged_events_are_grouped_by_change_owner() {
        val eventMatchingTransformer = EventGroupingTransformer(listOf(
                ChangeMatcher("*", "*", "*", "channel")
        ))
        val groups = getEventGroupsWithTransformer(eventMatchingTransformer)
        val bender = "bender"

        // For this test to be useful the test data needs to have more than one change merged event for the same user.
        val changeMergedEvents = groups.flatMap { it.events }.filterIsInstance<ChangeMergedEvent>()
        assertTrue("Bender needs more than one changes merged for this test to be valid",
                changeMergedEvents.groupBy { it.change.owner }.any { it.key.username == bender && it.value.size > 1 })

        assertTrue("There should be a group of change merged events from different submitters on commits owned by Bender",
                groups.any {
                    it.type == ChangeMergedEvent::class.java
                            && it.username == bender
                            && it.events.all { it.change.owner.username == bender }
                })
    }

    private fun getEventGroupsWithTransformer(eventGroupingTransformer: EventGroupingTransformer): List<EventGroup<*>> =
            this::class.java.getResourceAsStream("test/json/events-for-matching.txt")
                    .bufferedReader().lineSequence().toFlowable()
                    .compose(stringToEvent)
                    .ofType<ChangeEvent>()
                    .buffer(Int.MAX_VALUE)
                    .compose(eventGroupingTransformer).blockingIterable().toList()
}
