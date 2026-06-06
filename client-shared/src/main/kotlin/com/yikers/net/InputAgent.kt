package com.yikers.net

// Decides what a client sends. Human ignores the world; bot reads it.
fun interface InputAgent {
    fun decide(world: WorldSnapshot, slot: Int, dt: Float): InputCommand
}
