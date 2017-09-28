package sims.michael.gerritslackbot.model

data class ChangeMatcher(
        val project: String,
        val branch: String,
        val subject: String,
        val channel: String?,
        private val isVerificationOnly: Boolean? = null,
        private val changeKind: String? = null
) {

    fun matches(event: ChangeEvent): Boolean {
        return project.toLowerCase() in listOf("*", event.change.project.toLowerCase())
                && branch.toLowerCase() in listOf("*", event.change.branch.toLowerCase())
                && (isVerificationOnly == null || isVerificationOnly == event.isVerificationOnly)
                && (changeKind == null || changeKind.toLowerCase() == event.changeKindOrNull?.toLowerCase())
                && (subject == "*" || event.change.subject?.safeMatches(subject) ?: false)
    }

    private val ChangeEvent.isVerificationOnly: Boolean
        get() {
            // True if there are one or more approvals and they all are verified ones
            return this is CommentAddedEvent && approvals?.all { it.type == "Verified" } == true
        }

    private val ChangeEvent.changeKindOrNull: String?
        get() = if (this is PatchSetCreatedEvent) patchSet.kind.toString() else null

    private fun String.safeMatches(regex: String) = try {
        // Prefix with (?i) for a case-insensitive match
        "(?i)$regex".toRegex().containsMatchIn(this)
    } catch (e: Exception) {
        false
    }
}
