package com.yikers.ecs.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// Marks a player ball and carries its slot (-1 if unset). Held here rather than in
// an Entity-keyed map: Fleks' Entity hashCode crashes on RoboVM, so no Entity may
// enter a hash structure. Snapshot stamps playerId straight from this.
class Player(val slot: Int = -1) : Component<Player> {
    override fun type() = Player
    companion object : ComponentType<Player>()
}
