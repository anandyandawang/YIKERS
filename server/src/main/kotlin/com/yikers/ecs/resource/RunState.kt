package com.yikers.ecs.resource

// Live run state shared across systems + screen.
class RunState {
    var score = 0
    var dead = false
    var startCamera = false

    // Rising kill-line = the world Y of the screen's BOTTOM edge (meters). The scroll
    // system advances it; each client centers its own camera on it (using its local
    // view height, never sent here). Init 0 (ground). Resets per run since RunState is
    // rebuilt each newRun().
    var scrollY = 0f
    var totalTime = 0f
    var lastPlatformY = 0f
    var highScore = 0
}
