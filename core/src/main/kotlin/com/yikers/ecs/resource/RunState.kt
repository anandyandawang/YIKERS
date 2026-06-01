package com.yikers.ecs.resource

// Live run state shared across systems + screen.
class RunState {
    var score = 0
    var dead = false
    var startCamera = false
    var totalTime = 0f
    var lastPlatformY = 0f
    var highScore = 0
    var lethalHit = false
}
