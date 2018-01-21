package sims.michael.gerritslackbot.slack

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

class SlackNotifier(
        private val httpClient: OkHttpClient,
        private val objectMapper: ObjectMapper,
        private val url: String
) {

    fun notify(message: SlackMessage) {
        val request = Request.Builder()
                .url(url)
                .method("POST", FormBody.Builder()
                        .add("payload", objectMapper.writeValueAsString(message))
                        .build())
                .build()
        val response = httpClient.newCall(request).execute()
        if (response.code() != 200) throw HttpException(response.code(), response.message(), response.body().string())
    }

    class HttpException(code: Int, message: String?, body: String?) : RuntimeException("$code $message $body")

}
