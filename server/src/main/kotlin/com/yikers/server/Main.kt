package com.yikers.server

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.yikers.net.DedicatedServer
import com.yikers.net.SessionConfig
import com.yikers.net.discovery.DEFAULT_TCP_PORT
import java.net.InetAddress

// Standalone LAN server: ./gradlew :server:run [-Dyikers.humans=2 -Dyikers.port=54000]
//
// Boots a headless libGDX app first (same trick the integration tests use) so the
// Box2D native loads before GameInstance opens a world, then starts a DedicatedServer
// and blocks. Roster / seed / port / name come from sysprops or env, mirroring
// BootConfig: yikers.humans, yikers.bots, yikers.seed, yikers.port, yikers.name.
fun main() {
    HeadlessApplication(object : ApplicationAdapter() {}, HeadlessApplicationConfiguration())

    val humans = (intOf("yikers.humans", "YIKERS_HUMANS") ?: DEFAULT_HUMANS).coerceAtLeast(1)
    val bots = (intOf("yikers.bots", "YIKERS_BOTS") ?: 0).coerceAtLeast(0)
    val seed = longOf("yikers.seed", "YIKERS_SEED")
    val port = intOf("yikers.port", "YIKERS_PORT") ?: DEFAULT_TCP_PORT
    val name = raw("yikers.name", "YIKERS_NAME") ?: defaultName()

    val cfg = SessionConfig(humans = humans, bots = bots, seed = seed)
    val server = DedicatedServer(name = name, tcpPort = port, cfg = cfg)
    server.start()
    println("YIKERS server '$name' listening on tcp ${server.port} (humans=$humans bots=$bots)")

    Runtime.getRuntime().addShutdownHook(Thread { server.stop() })
    Thread.currentThread().join() // block forever; shutdown hook stops the server
}

private const val DEFAULT_HUMANS = 2

private fun defaultName(): String =
    "YIKERS @ " + runCatching { InetAddress.getLocalHost().hostName }.getOrDefault("server")

private fun raw(prop: String, env: String): String? =
    System.getProperty(prop) ?: System.getenv(env)

private fun intOf(prop: String, env: String) = raw(prop, env)?.trim()?.toIntOrNull()

private fun longOf(prop: String, env: String) = raw(prop, env)?.trim()?.toLongOrNull()
