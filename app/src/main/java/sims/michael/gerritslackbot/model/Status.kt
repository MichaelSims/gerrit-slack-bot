package sims.michael.gerritslackbot.model

enum class Status {

    /**
     * Change is open and pending review, or review is in progress.
     *
     * This is the default state assigned to a change when it is first created in the database. A
     * change stays in the NEW state throughout its review cycle, until the change is submitted or
     * abandoned.
     *
     * Changes in the NEW state can be moved to:
     *
     *  * [MERGED] - when the Submit Patch Set action is used;
     *  * [ABANDONED] - when the Abandon action is used.
     */
    NEW,

    /**
     * Change is a draft change that only consists of draft patchsets.
     *
     * This is a change that is not meant to be submitted or reviewed yet. If the uploader
     * publishes the change, it becomes a NEW change. Publishing is a one-way action, a change
     * cannot return to DRAFT status. Draft changes are only visible to the uploader and those
     * explicitly added as reviewers.
     *
     * Changes in the DRAFT state can be moved to:
     *
     *  * [NEW] - when the change is published, it becomes a new change;
     */
    DRAFT,

    /**
     * Change has been submitted and is in the merge queue. It may be waiting for one or more dependencies.
     */
    SUBMITTED,

    /**
     * Change is closed, and submitted to its destination branch.
     *
     * Once a change has been merged, it cannot be further modified by adding a replacement patch
     * set. Draft comments however may be published, supporting a post-submit review.
     */
    MERGED,

    /**
     * Change is closed, but was not submitted to its destination branch.
     *
     * Once a change has been abandoned, it cannot be further modified by adding a replacement
     * patch set, and it cannot be merged. Draft comments however may be published, permitting
     * reviewers to send constructive feedback.
     */
    ABANDONED

}
