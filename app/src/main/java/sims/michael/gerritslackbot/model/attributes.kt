package sims.michael.gerritslackbot.model

import com.fasterxml.jackson.annotation.JsonProperty

data class AccountAttribute(val name: String?, val email: String?, val username: String)

data class ApprovalAttribute(
        val type: String?,
        val description: String?,
        val value: String?,
        val oldValue: String?,
        val grantedOn: Long?,
        val by: AccountAttribute?
)

data class ChangeAttribute(
        val project: String,
        val branch: String,
        val topic: String?,
        val id: String?,
        val number: Int = 0,
        val subject: String?,
        val owner: AccountAttribute,
        val assignee: AccountAttribute?,
        val url: String?,
        val commitMessage: String?,
        val createdOn: Long?,
        val lastUpdated: Long?,
        val open: Boolean?,
        val status: Status?,
        val comments: List<MessageAttribute>?,
        val trackingIds: List<TrackingIdAttribute>?,
        val currentPatchSet: PatchSetAttribute?,
        val patchSets: List<PatchSetAttribute>?,
        val dependsOn: List<DependencyAttribute>?,
        val neededBy: List<DependencyAttribute>?,
        val submitRecords: List<SubmitRecordAttribute>?,
        val allReviewers: List<AccountAttribute>?
)

data class DependencyAttribute(
        val id: String?,
        val number: Int = 0,
        val revision: String?,
        val ref: String?,
        @get:JsonProperty("isCurrentPatchSet") val isCurrentPatchSet: Boolean?
)

data class MessageAttribute(val timestamp: Long?, val reviewer: AccountAttribute?, val message: String?)

data class PatchAttribute(
        val file: String?,
        val fileOld: String?,
        val type: ChangeType?,
        val insertions: Int = 0,
        val deletions: Int = 0
)

data class PatchSetAttribute(
        val number: Int = 0,
        val revision: String?,
        val parents: List<String>?,
        val ref: String?,
        val uploader: AccountAttribute,
        val createdOn: Long?,
        val author: AccountAttribute?,
        @get:JsonProperty("isDraft") val isDraft: Boolean = false,
        val kind: ChangeKind?,
        val approvals: List<ApprovalAttribute>?,
        val comments: List<PatchSetCommentAttribute>?,
        val files: List<PatchAttribute>?,
        val sizeInsertions: Int = 0,
        val sizeDeletions: Int = 0
)

data class PatchSetCommentAttribute(
        val file: String?,
        val line: Int?,
        val reviewer: AccountAttribute?,
        val message: String?
)

data class RefUpdateAttribute(val oldRev: String?, val newRev: String?, val refName: String?, val project: String?)

data class SubmitLabelAttribute(val label: String?, val status: String?, val by: AccountAttribute?)

data class SubmitRecordAttribute(val status: String?, val labels: List<SubmitLabelAttribute>?)

data class TrackingIdAttribute(val system: String?, val id: String?)
