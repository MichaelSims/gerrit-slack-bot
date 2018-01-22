package sims.michael.gerritslackbot

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import sims.michael.gerritslackbot.container.ObjectMappers
import sims.michael.gerritslackbot.container.defaultModule
import sims.michael.gerritslackbot.model.ChangeMatcher
import sims.michael.gerritslackbot.model.Config
import sims.michael.gerritslackbot.slack.EventGroupToSlackMessageTransformer

class YamlTests {

    private lateinit var objectMapper: ObjectMapper

    @Before
    fun setUp() {
        val container = Kodein {
            import(defaultModule)
        }
        objectMapper = container.instance(ObjectMappers.YAML)
    }

    @Test
    fun can_deserialize() {
        val config = objectMapper.readValue(this::class.java.getResourceAsStream("config.yaml"), Config::class.java)
        assertEquals("https://hooks.slack.example.com/services/id", config.slackApiUrl)
        assertEquals(15L, config.eventBufferTimeoutInSeconds)
        assertEquals(EventGroupToSlackMessageTransformer.Config(
                username = "Gerrit Review Bot",
                iconUrl = "http://www.example.com/icon.png",
                directMessagesEnabled = false,
                mergedChangeEmojiList = listOf(
                        ":clapping:", ":parrotbeer:", ":beers:", ":bomb:", ":animal:", ":badger:", ":homerscream:",
                        ":squirrel:", ":success:", ":arrrggghhh:", ":aw_yeah:", ":blondesassyparrot:", ":borat:",
                        ":bowdown:", ":celebrate:", ":dancing-penguin:", ":dancing_penguin:", ":dancingfood:",
                        ":headbang2:"
                )
        ), config.slackMessageConfig)
        assertEquals(SSHStreamPublisher.Config(
                host = "gerrit.example.com",
                port = 2222,
                username = "username",
                privateKeyLocations = listOf("/home/gerrit-slack-bot/.ssh/gerrit_service_rsa")
        ),
                config.gerritStreamConfig)
        assertEquals(mapOf("gerrit_username" to "slack_username"), config.gerritToSlackMap)
        assertEquals(listOf(
                ChangeMatcher("*", "*", "*", null),
                ChangeMatcher("*", "*", "^WIP: ", null),
                ChangeMatcher("*", "*", "*", null, true),
                ChangeMatcher("project-one", "feature-branch", "*", "#feature-branch-channel", changeKind = "REWORK"),
                ChangeMatcher("project-two", "*", "*", "#other-channel", changeKind = "REWORK")
        ), config.changeMatchers)
    }

    @Test
    fun can_serialize() {
        val config = Config(
                listOf(
                        ChangeMatcher("*", "*", "*", "#channel"),
                        ChangeMatcher("*", "branch", "*", null)
                ),
                mapOf("gerrit" to "slack", "chocolate" to "peanut butter"),
                SSHStreamPublisher.Config(
                        host = "example.com",
                        username = "username",
                        privateKeyLocations = listOf("somePath", "someOtherPath")
                ),
                "https://example.com",
                EventGroupToSlackMessageTransformer.Config(
                        username = "username",
                        iconUrl = "iconUrl",
                        directMessagesEnabled = false,
                        mergedChangeEmojiList = listOf(":one:", ":two:")
                ),
                Config.HttpConfig(true, "localhost"),
                30
        )
        val serialized = objectMapper.writeValueAsString(config)
        println(serialized)
    }
}
