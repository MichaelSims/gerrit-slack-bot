package sims.michael.gerritslackbot

import org.apache.commons.lang3.time.StopWatch
import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T : Any> T.logger(): Logger {
    return LoggerFactory.getLogger(T::class.java.simpleName)
}

inline fun <T> Logger.logTiming(label: String, block: () -> T): T {

    if (!isTraceEnabled) {
        return block()
    }

    val timer = StopWatch()
    timer.start()
    return try {
        block()
    } finally {
        timer.stop()
        debug("$label completed in $timer")
    }
}
