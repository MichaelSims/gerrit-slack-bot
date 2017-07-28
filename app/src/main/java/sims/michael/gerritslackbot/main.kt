@file:JvmName("Main")

package sims.michael.gerritslackbot

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.provider
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import io.reactivex.Flowable
import io.reactivex.rxkotlin.ofType
import io.reactivex.schedulers.Schedulers
import io.reactivex.subscribers.DisposableSubscriber
import net.schmizz.sshj.userauth.UserAuthException
import org.slf4j.LoggerFactory
import sims.michael.gerritslackbot.container.defaultModule
import sims.michael.gerritslackbot.model.ChangeEvent
import sims.michael.gerritslackbot.model.Config
import sims.michael.gerritslackbot.slack.EventGroupToSlackMessageTransformer
import sims.michael.gerritslackbot.slack.SlackMessage
import sims.michael.gerritslackbot.slack.SlackNotifier
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {

    mainBody("gerrit-slack-bot") {

        // Parse config file
        val configFile = File(ScriptArgs(ArgParser(args)).configFile)
        if (!configFile.exists()) {
            throw FileNotFoundException("${ScriptArgs(ArgParser(args)).configFile} doesn't exist")
        }

        // Initialize DI container
        val container = Kodein {
            bind<InputStream>() with provider { configFile.inputStream() }
            import(defaultModule)
        }

        // Initialize components
        val logger = LoggerFactory.getLogger("main")

        val slackNotifier: SlackNotifier = container.instance()
        val streamPublisher: Flowable<String> = container.instance()
        val stringToEventTransformer: StringToEventTransformer = container.instance()
        val eventGroupingTransformer: EventGroupingTransformer = container.instance()
        val eventGroupToSlackMessageTransformer: EventGroupToSlackMessageTransformer = container.instance()
        val config: Config = container.instance()

        // Start processing stream events
        while (true) {
            val subscriber = object : DisposableSubscriber<SlackMessage>() {
                override fun onComplete() = Unit
                override fun onNext(message: SlackMessage) = slackNotifier.notify(message)
                override fun onError(t: Throwable) = throw t
            }
            try {
                streamPublisher
                        .subscribeOn(Schedulers.io())
                        .doOnSubscribe { logger.debug("Subscribed to event stream") }
                        .flatMap { s ->
                            // Log and drop events that can't be parsed
                            Flowable.just(s).compose(stringToEventTransformer)
                                    .doOnError { e -> logger.error("JSON mapping error, skipping", e) }
                                    .onErrorResumeNext(Flowable.empty())
                        }
                        .ofType<ChangeEvent>()
                        .doOnNext { event -> logger.debug("Received event $event") }
                        .filter { event ->
                            val matcher = config.changeMatchers.find { matcher -> matcher.matches(event) }
                            if (matcher != null) {
                                logger.debug("$event matches $matcher")
                                matcher.channel != null
                            } else {
                                logger.debug("No match for $event")
                                false
                            }
                        }
                        .debounceBuffer(config.eventBufferTimeoutInSeconds, TimeUnit.SECONDS)
                        .doOnNext { list ->
                            logger.debug("Buffer timeout reached. Matching ${list.size} ${eventOrEvents(list.size)}")
                        }
                        .compose(eventGroupingTransformer)
                        .compose(eventGroupToSlackMessageTransformer)
                        .blockingSubscribe(subscriber)
            } catch (e: UserAuthException) {
                logger.error("Cannot authenticate, config is ${config.gerritStreamConfig}, terminating")
                throw e
            } catch (e: Exception) {
                logger.error("Caught exception processing event stream, restarting", e)
            } finally {
                subscriber.dispose()
            }
        }
    }

}

class ScriptArgs(parser: ArgParser) {
    val configFile by parser.storing("-c", "--configFile", help = "path to the config file")
}

private fun eventOrEvents(num: Int) = if (num == 1) "event" else "events"
