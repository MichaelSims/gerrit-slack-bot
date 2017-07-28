package sims.michael.gerritslackbot

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.toFlowable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import sims.michael.gerritslackbot.container.ObjectMappers
import sims.michael.gerritslackbot.container.defaultModule
import sims.michael.gerritslackbot.model.ChangeEvent
import sims.michael.gerritslackbot.model.ChangeMatcher
import sims.michael.gerritslackbot.model.EventGroup

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
        assertEquals(1, groups.size)
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

    private fun getEventGroupsWithTransformer(eventGroupingTransformer: EventGroupingTransformer): List<EventGroup<*>> =
            this::class.java.getResourceAsStream("../test/json/events-for-matching.txt")
                    .bufferedReader().lineSequence().toFlowable()
                    .compose(stringToEvent)
                    .ofType<ChangeEvent>()
                    .buffer(Int.MAX_VALUE)
                    .compose(eventGroupingTransformer).blockingIterable().toList()
}
