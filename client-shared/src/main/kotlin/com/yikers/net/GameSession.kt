package com.yikers.net

// A client's handle to ONE run: THE SEAM. Everything a client knows arrives here.
interface GameSession {
    val slot: Int                 // this client's seat, assigned at join
    val config: SessionConfig     // run params, handed over at join

    fun submitInput(cmd: InputCommand)
    fun snapshot(): WorldSnapshot
    fun close()
}
