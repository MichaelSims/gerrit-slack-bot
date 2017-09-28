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
        val projectMatches = project.toLowerCase() in listOf("*", event.change.project.toLowerCase())
        val branchMatches = branch.toLowerCase() in listOf("*", event.change.branch.toLowerCase())
        val isVerificationOnlyMatches = isVerificationOnly == null || isVerificationOnly == event.isVerificationOnly
        val eventChangeKind = event.changeKindOrNull
        val changeKindMatches = changeKind == null || eventChangeKind == null
                || changeKind.toLowerCase() == eventChangeKind.toLowerCase()
        val subjectMatches = subject == "*" || event.change.subject?.safeMatches(subject) ?: false
        return projectMatches && branchMatches && isVerificationOnlyMatches && changeKindMatches && subjectMatches
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
