package sims.michael.gerritslackbot.slack

import okhttp3.HttpUrl

private val gerritBaseUrlRegex = "^(http.*)/(\\d+)$".toRegex()

fun formatGerritBaseUrl(changeUrl: String): String? {
    return gerritBaseUrlRegex.matchEntire(changeUrl)?.groupValues?.get(1)
            ?: formatGerritBaseUrlUsingHttpUrl(changeUrl)
}

private fun formatGerritBaseUrlUsingHttpUrl(changeUrl: String): String? {
    val parsed = HttpUrl.parse(changeUrl) ?: return null
    val port = parsed.port().takeUnless { it == HttpUrl.defaultPort(parsed.scheme()) }?.let { ":$it" } ?: ""
    return "${parsed.scheme()}://${parsed.host()}$port"
}

fun formatGerritChangeUrl(baseUrl: String, changeNumber: Int, patchSetNumber: Int?): String?
        = "$baseUrl/$changeNumber/$patchSetNumber"
