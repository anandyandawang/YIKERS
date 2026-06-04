package com.yikers.ecs.resource

import com.github.quillraven.fleks.Entity

class Refs {
    var player: Entity? = null
    val boulders = ArrayList<Entity>()
    var nextBoulder = 0
}
