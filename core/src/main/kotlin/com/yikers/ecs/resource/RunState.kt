package com.yikers.ecs.resource

import com.yikers.config.GameConfig

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
}
