package sims.michael.gerritslackbot.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import io.reactivex.Flowable
import io.reactivex.rxkotlin.toFlowable
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import sims.michael.gerritslackbot.StringToEventTransformer
import sims.michael.gerritslackbot.container.ObjectMappers
import sims.michael.gerritslackbot.container.defaultModule
import sims.michael.gerritslackbot.logger

class DeserializationTests {

    private val logger = logger()

    private lateinit var objectMapper: ObjectMapper
    private lateinit var stringToEventTransformer: StringToEventTransformer

    @Before
    fun setUp() {
        val container = Kodein {
            import(defaultModule)
        }
        objectMapper = container.instance(ObjectMappers.JSON)
        stringToEventTransformer = container.instance()
    }

    @Test
    fun testAbandon() {
        val event = deserialize<ChangeAbandonedEvent>("abandon.json")
        assertEquals("fry@planex.3000", event.abandoner.email)
    }

    @Test
    fun testMerge() {
        val event = deserialize<ChangeMergedEvent>("merge.json")
        assertEquals("2352355sdfa", event.patchSet.revision)
    }

    @Test
    fun testPush() {
        val event = deserialize<PatchSetCreatedEvent>("push.json")
        assertEquals("234asdfasdf", event.patchSet.parents!!.first())
    }

    @Test
    fun testPushElse() {
        val event = deserialize<PatchSetCreatedEvent>("push-else.json")
        assertEquals("Baked potato", event.change.subject)
    }

    @Test
    fun testReview() {
        val event = deserialize<CommentAddedEvent>("review.json")
        assertEquals("-1", event.approvals!!.first().value)
    }

    @Test
    fun testUpdateRef() {
        val event = deserialize<RefUpdatedEvent>("update-ref.json")
        assertEquals("test/other", event.refUpdate.project)
    }

    @Test
    fun testUnknown() {
        val event = deserialize<UnknownEvent>("unknown.json")
        assertEquals("phillipjfry", (event.nodes["abandoner"] as Map<*, *>)["username"])
    }

    private data class LineError(val line: IndexedValue<String>, val error: Throwable)

    @Test
    fun testBulkDeserialization() {
        val stream = this::class.java.getResourceAsStream("../test/json/bulk-events.txt")
        val errors = mutableListOf<LineError>()
        val eventStream: Flowable<Event> = stream.bufferedReader().lineSequence().withIndex().toFlowable()
                .flatMap { line: IndexedValue<String> ->
                    Flowable.just(line.value)
                            .compose(stringToEventTransformer)
                            .doOnError { e ->
                                logger.error("Failed to parse line ${line.index + 1}", e)
                                errors.add(LineError(line, e))
                            }
                            .onErrorResumeNext(Flowable.empty())
                }
        val list = eventStream.toList().blockingGet()
        if (errors.isNotEmpty()) Assert.fail("${errors.size} occurred during parsing")
        logger.info("Successfully parsed ${list.size} events")
    }

    private inline fun <reified T : Event> deserialize(fileName: String): T {
        val stream = this::class.java.getResourceAsStream("../test/json/$fileName")
        return objectMapper.readValue(stream, Event::class.java) as T
    }
}
