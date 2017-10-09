package sims.michael.gerritslackbot

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.provider
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.toFlowable
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import sims.michael.gerritslackbot.container.ObjectMappers
import sims.michael.gerritslackbot.container.defaultModule
import sims.michael.gerritslackbot.model.ChangeEvent
import sims.michael.gerritslackbot.model.Config
import sims.michael.gerritslackbot.slack.EventGroupToSlackMessageTransformer
import sims.michael.gerritslackbot.slack.SlackNotifier
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream

class SlackIntegrationTest {

    private lateinit var config: Config
    private lateinit var stringToEvent: StringToEventTransformer
    private lateinit var objectMapper: ObjectMapper
    private lateinit var eventGroupingTransformer: EventGroupingTransformer
    private lateinit var eventGroupToSlackMessageTransformer: EventGroupToSlackMessageTransformer
    private lateinit var slackNotifier: SlackNotifier

    private val eventJsonStream: InputStream
        get () = this::class.java.getResourceAsStream("test/json/slack-integration-test-events.txt")

    @Before
    fun setUp() {
        val implicitConfigurationPath = File("${SlackIntegrationTest::class.simpleName}.yaml")
        if (!implicitConfigurationPath.exists()) {
            throw FileNotFoundException("$implicitConfigurationPath must point to a valid app config file")
        }
        val container = Kodein {
            bind<InputStream>() with provider { implicitConfigurationPath.inputStream() }
            import(defaultModule)
        }
        config = container.instance()
        stringToEvent = container.instance()
        objectMapper = container.instance(ObjectMappers.YAML)
        eventGroupingTransformer = container.instance()
        eventGroupToSlackMessageTransformer = container.instance()
        slackNotifier = container.instance()
    }

    @Test
    fun testEventsResourceExists() {
        assertNotNull(eventJsonStream)
    }

    @Test
    fun sendEvents() {
        eventJsonStream.bufferedReader().lineSequence().toFlowable().compose(stringToEvent)
                .ofType<ChangeEvent>().buffer(Int.MAX_VALUE)
                .compose(eventGroupingTransformer)
                .compose(eventGroupToSlackMessageTransformer)
                .subscribe { message -> slackNotifier.notify(message) }
    }
}
