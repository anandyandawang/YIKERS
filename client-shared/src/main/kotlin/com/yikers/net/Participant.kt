package com.yikers.net

import com.yikers.net.wire.AugmentPick

// One client = a session (transport) + an agent (brain). pump() = read, decide, send.
// autoSkipAugments: brains with no UI (bots) skip any offer so they don't freeze the
// room; the human client resolves offers through its own screen instead.
class Participant(
    val session: GameSession,
    private val agent: InputAgent,
    private val autoSkipAugments: Boolean = false,
) {
    fun pump(dt: Float) {
        val snap = session.snapshot()
        if (autoSkipAugments && snap.augmentOffers.any { it.slot == session.slot }) {
            session.submitAugmentPick(AugmentPick())
        }
        val cmd = agent.decide(snap, session.slot, dt)
        session.submitInput(cmd.copy(slot = session.slot))
    }

    fun close() = session.close()
}
