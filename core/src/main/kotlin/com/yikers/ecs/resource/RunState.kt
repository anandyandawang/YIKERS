package com.yikers.ecs.resource

import com.yikers.config.GameConfig

// Live run state shared across systems + screen.
class RunState {
    var score = 0
    var dead = false
    var startCamera = false

    // Generic sim-freeze effect: sim-advancing systems early-return while set, while
    // RenderSystem still draws so the scene reads frozen. DraftSystem raises it while
    // a human is mid-draft (see Draft.isAwaitingHuman); a later pause source could
    // raise it too -- systems freeze without caring why.
    var paused = false

    // Rising kill-line = the world Y of the screen's BOTTOM edge (meters). The
    // scroll system advances it; the camera shows [scrollY, scrollY + viewHeight],
    // so the bottom is anchored to the kill-line and taller screens see more world
    // above. Init 0 (ground). Resets per run since RunState is rebuilt each newRun().
    var scrollY = 0f
    var totalTime = 0f
    var lastPlatformY = 0f
    var highScore = 0

    // Visible world height (meters), driven by the device aspect via ExtendViewport.
    // Default = design height; PlayScreen updates it from the live viewport.
    var viewHeight = GameConfig.HEIGHT
}
