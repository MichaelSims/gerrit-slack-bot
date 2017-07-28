package sims.michael.gerritslackbot

import io.reactivex.Flowable
import java.util.concurrent.TimeUnit

/**
 * Buffer upstream items as "bursts". A burst is a group of item emissions that are followed by
 * a minimum amount of time with no more emissions.
 *
 * Inspired by [this gist](https://gist.github.com/benjchristensen/e4524a308456f3c21c0b).
 */
fun <T> Flowable<T>.debounceBuffer(timeout: Long, timeUnit: TimeUnit): Flowable<List<T>>
        = publish { upstream -> upstream.buffer(upstream.debounce(timeout, timeUnit)) }
