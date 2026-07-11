package com.example.biblelog.simulation.action

import com.example.biblelog.data.remote.ApiCreateCommentRequestDto
import com.example.biblelog.data.remote.ApiToggleReactionRequestDto
import com.example.biblelog.data.remote.ApiUpsertJournalNoteRequestDto
import com.example.biblelog.simulation.user.TestUser
import kotlin.random.Random

data class ActionContext(
    val random: Random,
    val knownNoteIds: List<String>,
    val knownUserIds: List<String>,
)

data class ActionResult(
    val success: Boolean,
    val metadata: Map<String, String> = emptyMap(),
)

interface UserAction {
    val name: String
    suspend fun execute(user: TestUser, context: ActionContext): ActionResult
}

class ActionRegistry(actions: List<UserAction>) {
    private val byName = actions.associateBy { it.name }

    fun get(name: String): UserAction =
        byName[name] ?: error("Unknown action: $name")

    fun names(): Set<String> = byName.keys
}

object BuiltinActions {
    val createPost = object : UserAction {
        override val name = "createPost"
        override suspend fun execute(user: TestUser, context: ActionContext): ActionResult {
            val note = user.createPost(
                content = "Simulation note ${context.random.nextInt()} from ${user.userId}",
            )
            return ActionResult(true, mapOf("noteId" to note.id))
        }
    }

    val like = object : UserAction {
        override val name = "like"
        override suspend fun execute(user: TestUser, context: ActionContext): ActionResult {
            val noteId = context.knownNoteIds.randomOrNull(context.random) ?: return ActionResult(false)
            user.likeNote(noteId)
            return ActionResult(true, mapOf("noteId" to noteId))
        }
    }

    val comment = object : UserAction {
        override val name = "comment"
        override suspend fun execute(user: TestUser, context: ActionContext): ActionResult {
            val noteId = context.knownNoteIds.randomOrNull(context.random) ?: return ActionResult(false)
            val comment = user.commentOnNote(noteId, "Grace and peace ${context.random.nextInt()}")
            return ActionResult(true, mapOf("noteId" to noteId, "commentId" to comment.id))
        }
    }

    val follow = object : UserAction {
        override val name = "follow"
        override suspend fun execute(user: TestUser, context: ActionContext): ActionResult {
            val target = context.knownUserIds
                .filter { it != user.userId }
                .randomOrNull(context.random) ?: return ActionResult(false)
            user.follow(target)
            return ActionResult(true, mapOf("targetUserId" to target))
        }
    }

    val refreshFeed = object : UserAction {
        override val name = "refreshFeed"
        override suspend fun execute(user: TestUser, context: ActionContext): ActionResult {
            val feed = user.refreshFeed()
            return ActionResult(true, mapOf("feedSize" to feed.size.toString()))
        }
    }

    fun all(): List<UserAction> = listOf(createPost, like, comment, follow, refreshFeed)

    fun defaultRegistry(): ActionRegistry = ActionRegistry(all())
}

private fun <T> List<T>.randomOrNull(random: Random): T? =
    if (isEmpty()) null else this[random.nextInt(size)]
