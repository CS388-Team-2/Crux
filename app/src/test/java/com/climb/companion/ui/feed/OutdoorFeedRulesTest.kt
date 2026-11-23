package com.climb.companion.ui.feed

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OutdoorFeedRulesTest {

    @Test
    fun firstClimbTagSkipsAreaTagsAndKeepsTheFirstClimb() {
        val tags = listOf(
            EntityTag(type = 1, targetId = "area", climbName = "", areaName = "Crag"),
            EntityTag(type = 0, targetId = "climb-a", climbName = "First", areaName = "Crag"),
            EntityTag(type = 0, targetId = "climb-b", climbName = "Second", areaName = "Crag"),
        )

        assertEquals("climb-a", firstClimbTag(tags)?.targetId)
        assertNull(firstClimbTag(listOf(tags.first())))
    }

    @Test
    fun chooseGradeUsesRopeGradeOtherwiseBoulderGrade() {
        assertEquals("5.10a", chooseGrade("5.10a", "V3"))
        assertEquals("V3", chooseGrade("  ", "V3"))
        assertEquals("V3", chooseGrade(null, "V3"))
        assertEquals("Ungraded", chooseGrade(null, null))
    }

    @Test
    fun nextCursorUsesTheLastEdgeAndStopsWhenThePageEnds() {
        assertEquals("cursor-b", nextCursor(listOf("cursor-a", "cursor-b"), hasNextPage = true))
        assertNull(nextCursor(listOf("cursor-b"), hasNextPage = false))
    }

    @Test
    fun photoUrlJoinsTheMediaHost() {
        assertEquals(
            "https://media.openbeta.io/u/photo.jpg",
            mediaUrl("/u/photo.jpg"),
        )
        assertEquals(
            "https://cdn.example/photo.jpg",
            mediaUrl("https://cdn.example/photo.jpg"),
        )
    }

    @Test
    fun weightedPickUsesTheSquareRootOfPhotoCount() {
        val states = listOf(
            OutdoorArea(uuid = "wa", name = "Washington", photoCount = 100),
            OutdoorArea(uuid = "ut", name = "Utah", photoCount = 1),
        )

        assertEquals("Washington", pickState(states, roll = 0.0)?.name)
        assertEquals("Utah", pickState(states, roll = 0.99)?.name)
        assertNull(pickState(listOf(states[1].copy(photoCount = 0)), roll = 0.0))
    }

    @Test
    fun sessionSkipsAreaTagsThenDedupesTheClimb() {
        val climbId = "35a66aac-59b3-48d4-8640-0e7f7f947b78"
        val source = FakeOutdoorSource(
            pages = mapOf(
                null to PhotoPage(
                    hasNextPage = true,
                    photos = listOf(
                        TaggedPhoto(
                            cursor = "c1",
                            mediaUrl = "/u/area.jpg",
                            tags = listOf(EntityTag(1, "area", "", "Icicle")),
                        )
                    ),
                ),
                "c1" to PhotoPage(
                    hasNextPage = false,
                    photos = listOf(
                        TaggedPhoto(
                            cursor = "c2",
                            mediaUrl = "/u/climb.jpg",
                            tags = listOf(
                                EntityTag(1, "area", "", "Icehouse"),
                                EntityTag(0, climbId, "Fat Lip", "Icehouse, The"),
                            ),
                        ),
                        TaggedPhoto(
                            cursor = "c3",
                            mediaUrl = "/u/climb-again.jpg",
                            tags = listOf(EntityTag(0, climbId, "Fat Lip", "Icehouse, The")),
                        ),
                    ),
                ),
            ),
            grades = mapOf(climbId to GradePair(yds = null, vscale = "V3")),
        )
        val session = OutdoorFeedSession(source, random = ZeroRandom())

        val cards = session.nextBatch()

        assertEquals(listOf(null, "c1"), source.requests)
        assertEquals(1, cards.size)
        assertEquals("Fat Lip", cards[0].name)
        assertEquals("V3", cards[0].grade)
        assertEquals("Icehouse, The", cards[0].areaName)
        assertEquals("https://media.openbeta.io/u/climb.jpg", cards[0].photoUrl)
    }
}

private class ZeroRandom : Random() {
    override fun nextBits(bitCount: Int): Int = 0
}

private class FakeOutdoorSource(
    private val pages: Map<String?, PhotoPage>,
    private val grades: Map<String, GradePair>,
) : OutdoorSource {
    val requests = mutableListOf<String?>()

    override fun loadStates(): List<OutdoorArea> = listOf(
        OutdoorArea(
            uuid = "11111111-1111-1111-1111-111111111111",
            name = "Washington",
            photoCount = 100,
        )
    )

    override fun photos(areaUuid: String, after: String?): PhotoPage {
        requests.add(after)
        return pages[after] ?: PhotoPage(hasNextPage = false, photos = emptyList())
    }

    override fun grades(climbIds: List<String>): Map<String, GradePair> =
        grades.filterKeys { it in climbIds }
}
