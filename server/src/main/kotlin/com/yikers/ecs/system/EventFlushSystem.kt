package com.yikers.ecs.system

import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.ecs.event.Events

// Must run last: events live exactly one tick.
class EventFlushSystem(
    private val events: Events = inject(),
) : IntervalSystem() {
    override fun onTick() {
        events.queue.clear()
    }
}
