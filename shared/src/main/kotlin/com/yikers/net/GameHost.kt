package com.yikers.net

// Identifies one room on a host.
@JvmInline
value class RoomId(val code: String)

data class RoomInfo(val id: RoomId, val players: Int, val open: Boolean)

// Owns many game worlds keyed by RoomId, so one server can host many games at once.
// LocalHost (in-process, usually a single room) implements this today; a future
// DedicatedHost serves many rooms over a socket behind the same interface. roomId
// is bound once at join, so per-frame InputCommand/WorldSnapshot stay room-free.
interface GameHost {
    fun open(cfg: SessionConfig): RoomId
    fun join(room: RoomId): GameSession
    fun list(): List<RoomInfo>
    fun close(room: RoomId)
}
