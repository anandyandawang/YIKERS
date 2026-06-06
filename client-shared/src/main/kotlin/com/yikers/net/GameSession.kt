package com.yikers.net

// A client's handle to ONE room: THE SEAM. Local + Network implement it identically.
interface GameSession {
    val slot: Int   // this client's seat, assigned at join

    fun submitInput(cmd: InputCommand)
    fun snapshot(): WorldSnapshot
    fun close()
}
