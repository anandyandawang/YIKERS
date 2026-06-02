package com.yikers.ecs.resource

import com.yikers.config.GameConfig
import com.yikers.ecs.EntityId

// Live run state shared across systems + screen.
class RunState {
    var score = 0
    var dead = false
    var startCamera = false

    // Domain view-bottom / rising kill-line (meters). The scroll system advances
    // it; the render camera follows it. Init = HEIGHT/2, matching the cam init in
    // PlayScreen.newRun(). Resets per run since RunState is rebuilt each newRun().
    var scrollY = GameConfig.HEIGHT / 2f
    var totalTime = 0f
    var lastPlatformY = 0f
    var highScore = 0

    // Climbers whose ball touched a hazard this run; DeathSystem kills them.
    // Keyed by EntityId (Int) not Entity: dodges an iOS/RoboVM crash; see EntityId.
    val lethalHits = HashSet<EntityId>()
}
