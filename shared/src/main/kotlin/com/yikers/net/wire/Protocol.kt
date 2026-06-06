package com.yikers.net.wire

import com.yikers.net.InputCommand
import com.yikers.net.SessionConfig
import com.yikers.net.WorldSnapshot
import kotlinx.serialization.Serializable

// LAN wire protocol: one sealed family. Flow: Join -> Welcome (assigns slot),
// then client Input per frame + server Snapshot per tick.
@Serializable
sealed interface Envelope

@Serializable
data class Join(val protocolVersion: Int = PROTOCOL_VERSION) : Envelope

@Serializable
data class Welcome(val slot: Int, val config: SessionConfig) : Envelope

// Server re-stamps cmd.slot so a client can't drive another's seat.
@Serializable
data class Input(val cmd: InputCommand) : Envelope

@Serializable
data class Snapshot(val world: WorldSnapshot) : Envelope

@Serializable
data class Rejected(val reason: String) : Envelope

const val PROTOCOL_VERSION = 1
