package com.climb.companion.ui.feed

import kotlin.math.sqrt
import kotlin.random.Random

const val CLIMB_TAG = 0

data class EntityTag(
    val type: Int,
    val targetId: String,
    val climbName: String,
    val areaName: String,
)

data class OutdoorArea(
    val uuid: String,
    val name: String,
    val photoCount: Int,
)

data class TaggedPhoto(
    val cursor: String,
    val mediaUrl: String,
    val tags: List<EntityTag>,
)

data class PhotoPage(
    val hasNextPage: Boolean,
    val photos: List<TaggedPhoto>,
)

data class GradePair(
    val yds: String?,
    val vscale: String?,
)

data class OutdoorCard(
    val climbId: String,
    val name: String,
    val grade: String,
    val areaName: String,
    val photoUrl: String,
)

interface OutdoorSource {
    fun loadStates(): List<OutdoorArea>
    fun photos(areaUuid: String, after: String?): PhotoPage
    fun grades(climbIds: List<String>): Map<String, GradePair>
}

fun firstClimbTag(tags: List<EntityTag>): EntityTag? =
    tags.firstOrNull { it.type == CLIMB_TAG && it.targetId.isNotBlank() }

fun chooseGrade(yds: String?, vscale: String?): String {
    val rope = yds?.trim().orEmpty()
    if (rope.isNotEmpty()) return rope
    val boulder = vscale?.trim().orEmpty()
    if (boulder.isNotEmpty()) return boulder
    return "Ungraded"
}

fun mediaUrl(path: String): String {
    val trimmed = path.trim()
    if (trimmed.startsWith("https://") || trimmed.startsWith("http://")) return trimmed
    val suffix = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
    return "https://media.openbeta.io$suffix"
}

fun nextCursor(cursors: List<String>, hasNextPage: Boolean): String? {
    if (!hasNextPage) return null
    return cursors.lastOrNull()?.takeIf { it.isNotBlank() }
}

fun pickState(states: List<OutdoorArea>, roll: Double): OutdoorArea? {
    val usable = states.filter { it.photoCount > 0 }
    if (usable.isEmpty()) return null
    val weights = usable.map { sqrt(it.photoCount.toDouble()) }
    val total = weights.sum()
    if (total <= 0.0) return null
    var left = roll.coerceIn(0.0, 1.0) * total
    for (index in usable.indices) {
        left -= weights[index]
        if (left < 0.0) return usable[index]
    }
    return usable.last()
}

class OutdoorFeedSession(
    private val source: OutdoorSource,
    private val random: Random = Random.Default,
    private val handful: Int = 6,
    private val maxPages: Int = 8,
) {
    private var states: List<OutdoorArea> = emptyList()
    private var statesLoaded = false
    private val cursors = mutableMapOf<String, String?>()
    private val exhausted = mutableSetOf<String>()
    private val seenPhotos = mutableSetOf<String>()
    private val seenClimbs = mutableSetOf<String>()

    fun hasMore(): Boolean = !statesLoaded || exhausted.size < states.size

    fun nextBatch(): List<OutdoorCard> {
        ensureStates()
        val collected = mutableListOf<OutdoorCard>()
        var pages = 0
        while (collected.size < handful && pages < maxPages && hasMore()) {
            val page = try {
                fetchOnePage()
            } catch (e: java.util.concurrent.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (collected.isEmpty()) throw e
                return collected
            }
            pages++
            if (page == null) break
            collected.addAll(page)
        }
        return collected
    }

    private fun ensureStates() {
        if (statesLoaded) return
        states = source.loadStates().filter { it.uuid.isNotBlank() && it.photoCount > 0 }
        statesLoaded = true
    }

    private fun fetchOnePage(): List<OutdoorCard>? {
        val available = states.filter { it.uuid !in exhausted }
        val state = pickState(available, random.nextDouble()) ?: return null
        val after = cursors[state.uuid]
        val page = source.photos(state.uuid, after)
        val next = nextCursor(page.photos.map { it.cursor }, page.hasNextPage)
        if (next == null || next == after) {
            exhausted.add(state.uuid)
        } else {
            cursors[state.uuid] = next
        }

        val pending = mutableListOf<OutdoorCard>()
        for (photo in page.photos) {
            val tag = firstClimbTag(photo.tags) ?: continue
            val url = mediaUrl(photo.mediaUrl)
            if (url.isBlank() || url in seenPhotos || tag.targetId in seenClimbs) continue
            seenPhotos.add(url)
            seenClimbs.add(tag.targetId)
            pending.add(
                OutdoorCard(
                    climbId = tag.targetId,
                    name = tag.climbName.ifBlank { "Climb" },
                    grade = "",
                    areaName = tag.areaName,
                    photoUrl = url,
                )
            )
        }
        if (pending.isEmpty()) return emptyList()
        val grades = source.grades(pending.map { it.climbId })
        return pending.map { card ->
            val pair = grades[card.climbId]
            card.copy(grade = chooseGrade(pair?.yds, pair?.vscale))
        }.shuffled(random)
    }
}
