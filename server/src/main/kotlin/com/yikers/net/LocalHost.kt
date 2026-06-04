package com.yikers.net

import com.yikers.sim.GameInstance

// In-process host. join() allocates a slot per call; opening a room is player-free.
class LocalHost : GameHost {
    private val rooms = LinkedHashMap<RoomId, GameInstance>()
    private var counter = 0

    override fun open(cfg: SessionConfig): RoomId {
        val id = RoomId("local-${counter++}")
        rooms[id] = GameInstance(cfg)
        return id
    }

    override fun join(room: RoomId): GameSession {
        val inst = instance(room)
        return LocalGameSession(inst, inst.addPlayer())
    }

    // For a host driving the clock itself (DedicatedServer).
    fun instance(room: RoomId): GameInstance =
        rooms[room] ?: error("no such room: ${room.code}")

    override fun list(): List<RoomInfo> =
        rooms.map { (id, inst) -> RoomInfo(id, inst.players, open = true) }

    override fun close(room: RoomId) {
        rooms.remove(room)?.close()
    }
}
