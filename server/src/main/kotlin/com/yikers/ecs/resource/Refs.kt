package com.yikers.ecs.resource

import com.github.quillraven.fleks.Entity

// Key entity handles for systems that cross-reference (player y, boulder pool).
class Refs {
    var player: Entity? = null
    val boulders = ArrayList<Entity>()
    var nextBoulder = 0
}
