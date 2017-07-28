package sims.michael.gerritslackbot

interface SlackNameResolver {
    operator fun get(key: String?): String?
}

class DefaultSlackNameResolver(private val usernameMap: Map<String, String>) : SlackNameResolver {
    override operator fun get(key: String?): String? =
            (usernameMap[key] ?: usernameMap["*"])?.let { if (it.startsWith("@")) it else "@$it" }
}
