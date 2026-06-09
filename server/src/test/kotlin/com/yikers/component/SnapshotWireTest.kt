package com.yikers.component

import com.yikers.net.AugmentSnap
import com.yikers.net.PlayerSnap
import com.yikers.net.PropSnap
import com.yikers.net.ShapeKind
import com.yikers.net.WorldSnapshot
import com.yikers.net.wire.AugmentOffer
import com.yikers.net.wire.AugmentPick
import com.yikers.net.wire.ResumePlay
import com.yikers.net.wire.Snapshot
import com.yikers.net.wire.Wire
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

// Sealed EntitySnap must survive the CBOR wire: a PlayerSnap stays a PlayerSnap
// (with its slot), a PropSnap stays a PropSnap (with its id).
class SnapshotWireTest {

    @Test
    fun sealedEntitySnapRoundTripsOverCbor() {
        val player = PlayerSnap(ShapeKind.CIRCLE, 1f, 0f, 0f, 1f, 2f, 3f, 0.5f, 0.5f, 0f, slot = 3)
        val prop = PropSnap(ShapeKind.CIRCLE, 0.5f, 0.5f, 0.5f, 1f, 4f, 5f, 0.6f, 0.6f, 0f, id = 42)
        val world = WorldSnapshot(
            tick = 7L,
            entities = listOf(player, prop),
            platforms = emptyList(),
            score = 0,
            dead = false,
            scrollY = 0f,
            highScore = 0,
        )

        val decoded = (Wire.decode(Wire.encode(Snapshot(world))) as Snapshot).world

        assertEquals(2, decoded.entities.size)
        val p = decoded.entities.filterIsInstance<PlayerSnap>().single()
        val o = decoded.entities.filterIsInstance<PropSnap>().single()
        assertEquals(3, p.slot)
        assertEquals(2f, p.x)
        assertEquals(42, o.id)
        assertEquals(5f, o.y)
    }

    @Test
    fun augmentPickRoundTripsOverCbor() {
        val pick = Wire.decode(Wire.encode(AugmentPick(augmentId = "double_jump", swapOutId = "old"))) as AugmentPick
        assertEquals("double_jump", pick.augmentId)
        assertEquals("old", pick.swapOutId)

        val skip = Wire.decode(Wire.encode(AugmentPick())) as AugmentPick
        assertNull(skip.augmentId) { "a skip carries no augment" }
        assertNull(skip.swapOutId)
    }

    @Test
    fun augmentOfferEventRoundTripsOverCbor() {
        val env = AugmentOffer(
            choices = listOf(AugmentSnap("double_jump", "Double Jump", "jump again in mid-air")),
            owned = emptyList(),
            maxOwned = 5,
        )
        val decoded = Wire.decode(Wire.encode(env)) as AugmentOffer
        assertEquals("double_jump", decoded.choices.single().id)
        assertEquals(5, decoded.maxOwned)

        // ResumePlay is a marker object; it must survive the sealed round-trip too.
        assert(Wire.decode(Wire.encode(ResumePlay)) is ResumePlay)
    }
}
