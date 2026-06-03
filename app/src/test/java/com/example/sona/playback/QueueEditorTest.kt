package com.example.sona.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class QueueEditorTest {
    @Test
    fun moveItem_movesItemToTargetIndex() {
        assertEquals(
            listOf("B", "C", "A", "D"),
            listOf("A", "B", "C", "D").moveItem(fromIndex = 0, toIndex = 2),
        )
    }

    @Test
    fun moveItem_ignoresInvalidIndex() {
        val queue = listOf("A", "B")

        assertEquals(queue, queue.moveItem(fromIndex = 3, toIndex = 0))
    }

    @Test
    fun removeIndex_removesItemAtIndex() {
        assertEquals(
            listOf("A", "C"),
            listOf("A", "B", "C").removeIndex(1),
        )
    }
}
