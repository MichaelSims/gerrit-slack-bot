package sims.michael.gerritslackbot.model

/** Type of modification made to the file path.  */
enum class ChangeType {
    /** Path is being created/introduced by this patch.  */
    ADDED,

    /** Path already exists, and has updated content.  */
    MODIFIED,

    /** Path existed, but is being removed by this patch.  */
    DELETED,

    /** Path existed but was moved.  */
    RENAMED,

    /** Path was copied from source.  */
    COPIED,

    /** Sufficient amount of content changed to claim the file was rewritten.  */
    REWRITE
}
