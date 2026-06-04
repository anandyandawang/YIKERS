package com.yikers.net

// A client's handle to ONE running game (room). THE SEAM: the client codes only
// against this. LocalGameSession implements it in-process today; a future
// NetworkClient implements the same interface over a socket, leaving client code
// untouched. The GameInstance behind it is never exposed to the client.
interface GameSession {
    // The player slot this client owns. Assigned at join (locally or by the server
    // handshake); every InputCommand this client submits is stamped with it.
    val playerId: Int

    fun submitInput(cmd: InputCommand)
    fun step(deltaTime: Float)
    fun snapshot(): WorldSnapshot
    fun close()
}
