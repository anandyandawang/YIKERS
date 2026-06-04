package com.yikers.net

import com.yikers.sim.GameInstance

// In-process host: singleplayer and a future LAN host both use this; it just holds
// one room locally. A future DedicatedHost serves many rooms over a socket behind
// the same GameHost interface, so client code never changes.
class LocalHost : GameHost {
    private val rooms = LinkedHashMap<RoomId, GameInstance>()
    private var counter = 0

    override fun open(cfg: SessionConfig): RoomId {
        val id = RoomId("local-${counter++}")
        rooms[id] = GameInstance(cfg)
        return id
    }

    override fun join(room: RoomId): GameSession =
        LocalGameSession(rooms[room] ?: error("no such room: ${room.code}"))

    override fun list(): List<RoomInfo> =
        rooms.map { (id, inst) -> RoomInfo(id, inst.players, open = true) }

    override fun close(room: RoomId) {
        rooms.remove(room)?.close()
    }
}
