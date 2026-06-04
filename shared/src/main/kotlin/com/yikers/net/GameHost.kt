package com.yikers.net

@JvmInline
value class RoomId(val code: String)

data class RoomInfo(val id: RoomId, val players: Int, val open: Boolean)

// Owns game worlds keyed by RoomId; roomId bound at join, so per-frame messages
// stay room-free.
interface GameHost {
    fun open(cfg: SessionConfig): RoomId
    fun join(room: RoomId): GameSession
    fun list(): List<RoomInfo>
    fun close(room: RoomId)
}
