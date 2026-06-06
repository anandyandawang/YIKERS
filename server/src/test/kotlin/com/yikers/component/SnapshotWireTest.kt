package com.yikers.component

import com.yikers.net.PlayerSnap
import com.yikers.net.PropSnap
import com.yikers.net.ShapeKind
import com.yikers.net.WorldSnapshot
import com.yikers.net.wire.Snapshot
import com.yikers.net.wire.Wire
import org.junit.jupiter.api.Assertions.assertEquals
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
}
