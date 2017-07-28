package sims.michael.gerritslackbot

import com.fasterxml.jackson.databind.ObjectMapper
import io.reactivex.Flowable
import io.reactivex.FlowableTransformer
import sims.michael.gerritslackbot.model.Event

class StringToEventTransformer(private val objectMapper: ObjectMapper) : FlowableTransformer<String, Event> {
    override fun apply(upstream: Flowable<String>): Flowable<Event>
            = upstream.map { s -> objectMapper.readValue(s, Event::class.java) }
}
