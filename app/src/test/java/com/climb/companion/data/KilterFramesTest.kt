package com.climb.companion.data

import org.junit.Assert.assertEquals
import org.junit.Test

class KilterFramesTest {

    private val roles = mapOf(
        12 to "start",
        13 to "middle",
        14 to "finish",
        15 to "foot",
    )

    @Test
    fun parseFramesIntoStartHandFinishAndFoot() {
        val frames = "p1145r12p1201r13p1392r14p1256r15"

        val holds = parseKilterFrames(frames)

        assertEquals(
            listOf(
                FrameHold(1145, 12),
                FrameHold(1201, 13),
                FrameHold(1392, 14),
                FrameHold(1256, 15),
            ),
            holds,
        )
        assertEquals(
            listOf("start", "hand", "finish", "foot"),
            holds.map { holdRole(it.roleId, roles) },
        )
    }

    @Test
    fun blankFramesHaveNoHolds() {
        assertEquals(emptyList<FrameHold>(), parseKilterFrames(""))
    }
}
