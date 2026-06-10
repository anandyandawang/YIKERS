package com.yikers.ecs.event

import com.github.quillraven.fleks.Entity

// Per-tick facts. Emit anywhere (system, contact listener); any LATER system reads
// them the same tick; EventFlushSystem clears at tick end. Nothing crosses ticks.
sealed interface GameEvent

data class LethalContact(val victim: Entity) : GameEvent
data class PlatformCleared(val platform: Entity, val scored: Int) : GameEvent
data class PlayerDied(val player: Entity) : GameEvent
data class RunEnded(val score: Int) : GameEvent

class Events {
    val queue = ArrayList<GameEvent>()

    fun emit(event: GameEvent) {
        queue += event
    }

    // Index loop, not iterator: a handler may emit while reading (append-only).
    inline fun <reified T : GameEvent> each(action: (T) -> Unit) {
        var i = 0
        while (i < queue.size) {
            (queue[i] as? T)?.let(action)
            i++
        }
    }
}
