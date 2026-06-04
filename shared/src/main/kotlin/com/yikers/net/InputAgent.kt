package com.yikers.net

// Decides what a client sends from what it sees. Human agent reads the keyboard
// (ignores world); bot agent reads the world. selfId = this client's slot.
fun interface InputAgent {
    fun decide(world: WorldSnapshot, selfId: Int, dt: Float): InputCommand
}
