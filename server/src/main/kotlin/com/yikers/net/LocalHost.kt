package com.yikers.net

import com.yikers.sim.GameInstance

// Room registry behind a DedicatedServer: opens GameInstances keyed by RoomId. The
// server drives the clock + allocates slots itself, so no join()/list() here.
class LocalHost {
    private val rooms = LinkedHashMap<RoomId, GameInstance>()
    private var counter = 0

    fun open(cfg: SessionConfig): RoomId {
        val id = RoomId("local-${counter++}")
        rooms[id] = GameInstance(cfg)
        return id
    }

    fun instance(room: RoomId): GameInstance =
        rooms[room] ?: error("no such room: ${room.code}")

    fun close(room: RoomId) {
        rooms.remove(room)?.close()
    }
}
