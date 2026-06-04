package com.yikers.net

// One client = a session (transport) + an agent (brain). Human or bot, local or
// socket — all the same. pump() is the whole loop body: read world, decide, send.
class Participant(
    val session: GameSession,
    private val agent: InputAgent,
) {
    fun pump(dt: Float) {
        val cmd = agent.decide(session.snapshot(), session.playerId, dt)
        session.submitInput(cmd.copy(playerId = session.playerId))
    }

    fun close() = session.close()
}
