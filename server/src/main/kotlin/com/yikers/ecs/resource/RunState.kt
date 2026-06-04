package com.yikers.ecs.resource

// Live run state shared across systems + screen.
class RunState {
    var score = 0
    var dead = false
    var startCamera = false

    // Flips true once at least one player has joined + spawned. The roster is
    // dynamic now (one player per client), so without this an empty world (no
    // clients yet) would read as "all climbers dead" the instant DeathSystem runs.
    // Run-end keys off this so a freshly-opened, still-empty room never ends.
    var started = false

    // Rising kill-line = the world Y of the screen's BOTTOM edge (meters). The scroll
    // system advances it; each client centers its own camera on it (using its local
    // view height, never sent here). Init 0 (ground). Resets per run since RunState is
    // rebuilt each newRun().
    var scrollY = 0f
    var totalTime = 0f
    var lastPlatformY = 0f
    var highScore = 0
}
