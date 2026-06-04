package com.yikers.bot

import com.yikers.config.RunConfig
import com.yikers.config.laneX
import com.yikers.net.GameSession
import com.yikers.net.InputCommand

// A bot IS a client. It holds a GameSession exactly like a human client does,
// reads the same WorldSnapshot every client sees, and ships its decision as an
// InputCommand — so to the server it is indistinguishable from a person. It never
// steps the sim (the clock owner does that, locally or the server remotely);
// whatever drives the run calls pump() once per frame.
class BotClient(
    private val session: GameSession,
    runConfig: RunConfig,
) {
    private val percept = SnapshotPercept(runConfig)
    private val brain = BotBrain()
    private val spawnHintX = laneX(session.playerId)

    fun pump(dt: Float) {
        val snap = session.snapshot()
        if (snap.entities.isEmpty()) return            // world not built / no ball yet
        percept.update(snap, dt, spawnHintX)
        val move = brain.decide(percept.self, percept.view)
        session.submitInput(InputCommand(session.playerId, move.vx, move.jump))
    }
}
