package com.yikers.net

// CLIENT-side decision contract — the mirror of the server's Controller. Given what
// this client can see (a WorldSnapshot) it decides what it sends (an InputCommand).
// A human agent reads the keyboard and ignores the world; a bot agent reads the
// world through its brain. `selfId` is this client's own slot, so an agent can find
// its own ball. The server never sees this type: it only ever receives the resulting
// InputCommand over the GameSession seam, so it cannot tell a human from a bot.
fun interface InputAgent {
    fun decide(world: WorldSnapshot, selfId: Int, dt: Float): InputCommand
}
