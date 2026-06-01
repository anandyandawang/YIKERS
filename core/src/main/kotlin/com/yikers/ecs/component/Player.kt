package com.yikers.ecs.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// Marker: the player entity.
class Player : Component<Player> {
    override fun type() = Player
    companion object : ComponentType<Player>()
}
