package com.yikers.ecs.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// Player ball + its slot. Held here not in an Entity-keyed map:
// Fleks' Entity hashCode crashes on RoboVM.
class Player(val slot: Int) : Component<Player> {
    override fun type() = Player
    companion object : ComponentType<Player>()
}
