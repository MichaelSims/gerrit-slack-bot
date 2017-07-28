package sims.michael.gerritslackbot.model

/** Operation performed by a change relative to its parent.  */
enum class ChangeKind {
    /** Nontrivial content changes.  */
    REWORK,

    /** Conflict-free merge between the new parent and the prior patch set.  */
    TRIVIAL_REBASE,

    /** Conflict-free change of first (left) parent of a merge commit.  */
    MERGE_FIRST_PARENT_UPDATE,

    /** Same tree and same parent tree.  */
    NO_CODE_CHANGE,

    /** Same tree, parent tree, same commit message.  */
    NO_CHANGE
}
