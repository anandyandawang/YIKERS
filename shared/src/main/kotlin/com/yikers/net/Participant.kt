package com.yikers.net

// One client = a transport handle (GameSession) + a decision policy (InputAgent).
// Human or bot, in-process or over a socket — all the same here. pump() is the whole
// client loop body: read the latest world, decide, send. Identical for everyone, so
// "human" vs "bot" is just which InputAgent you pass. The session owns the seam to
// the server; the agent owns the brain. The server, on the far side of the session,
// sees only InputCommands and so never learns which kind of client this is.
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
