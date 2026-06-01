package com.yikers.ecs.resource

import com.github.quillraven.fleks.Entity

// Live run state shared across systems + screen.
class RunState {
    var score = 0
    var dead = false
    var startCamera = false
    var totalTime = 0f
    var lastPlatformY = 0f
    var highScore = 0

    // Climbers whose ball touched a hazard this run; DeathSystem kills them.
    val lethalHits = HashSet<Entity>()
}
