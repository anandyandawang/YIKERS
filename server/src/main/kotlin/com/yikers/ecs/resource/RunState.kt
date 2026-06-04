package com.yikers.ecs.resource

// Live run state shared across systems + screen.
class RunState {
    var score = 0
    var dead = false
    var startCamera = false

    // True once a player spawned, so an empty room doesn't read as "everyone died".
    var started = false

    // Rising kill-line: world Y of the screen's bottom edge (meters).
    var scrollY = 0f
    var totalTime = 0f
    var lastPlatformY = 0f
    var highScore = 0
}
