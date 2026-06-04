package com.yikers.net.wire

import com.yikers.net.InputCommand
import com.yikers.net.SessionConfig
import com.yikers.net.WorldSnapshot
import kotlinx.serialization.Serializable

// The LAN wire protocol. One @Serializable sealed family, so kotlinx-serialization
// does closed polymorphism (a tag byte) with no manual registration. Every TCP
// frame carries exactly one Envelope, CBOR-encoded, length-prefixed (see Framing).
//
// Flow: client connects -> sends Join -> server replies Welcome (assigns playerId +
// run params). Then per frame the client sends Input, and every server tick the
// server broadcasts Snapshot. The server owns the clock; the client never ticks.
@Serializable
sealed interface Envelope

// client -> server, once right after the TCP connect.
@Serializable
data class Join(val protocolVersion: Int = PROTOCOL_VERSION) : Envelope

// server -> client, the reply to Join: this connection's player slot + run config.
@Serializable
data class Welcome(val playerId: Int, val config: SessionConfig) : Envelope

// client -> server, one per client frame. The server re-stamps cmd.playerId with the
// connection's assigned id before relaying, so a client can't drive another's slot.
@Serializable
data class Input(val cmd: InputCommand) : Envelope

// server -> client, one per broadcast tick: the authoritative frame to render.
@Serializable
data class Snapshot(val world: WorldSnapshot) : Envelope

// server -> client, sent once if the connect is refused (e.g. room full). The client
// surfaces the reason and drops back to the lobby.
@Serializable
data class Rejected(val reason: String) : Envelope

const val PROTOCOL_VERSION = 1
