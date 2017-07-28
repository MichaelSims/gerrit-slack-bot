package sims.michael.gerritslackbot.model

data class ChangeMatcher(
        val project: String,
        val branch: String,
        val subject: String,
        val channel: String?,
        private val isVerification: Boolean? = null
) {

    fun matches(event: ChangeEvent): Boolean {
        return project.toLowerCase() in listOf("*", event.change.project.toLowerCase())
                && branch.toLowerCase() in listOf("*", event.change.branch.toLowerCase())
                && (isVerification == null || isVerification == event.isVerification)
                && (subject == "*" || event.change.subject?.safeMatches(subject) ?: false)
    }

    private val ChangeEvent.isVerification: Boolean
        get() = this is CommentAddedEvent && approvals.orEmpty().any { it.type == "Verified" }

    private fun String.safeMatches(regex: String) = try {
        // Prefix with (?i) for a case-insensitive match
        "(?i)$regex".toRegex().containsMatchIn(this)
    } catch (e: Exception) {
        false
    }
}
