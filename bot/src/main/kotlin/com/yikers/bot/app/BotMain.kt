package com.yikers.bot.app

import com.yikers.net.discovery.DEFAULT_TCP_PORT

// Standalone bot launcher: connects N bot clients to a running server over the
// socket. Pure JVM — no libGDX, no sim — because a bot is just a client (reads
// snapshots, sends input). Run AFTER a server is up:
//   ./gradlew :bot:run        (env: YIKERS_HOST, YIKERS_PORT, YIKERS_BOTS)
// The server it connects to has no bot concept at all; these are ordinary clients.
fun main() {
    val host = raw("yikers.host", "YIKERS_HOST") ?: "127.0.0.1"
    val port = intOf("yikers.port", "YIKERS_PORT") ?: DEFAULT_TCP_PORT
    val count = (intOf("yikers.bots", "YIKERS_BOTS") ?: 1).coerceAtLeast(1)

    val runner = BotRunner(host, port, count)
    runner.start()
    println("YIKERS bots: $count connected to $host:$port")

    Runtime.getRuntime().addShutdownHook(Thread { runner.stop() })
    Thread.currentThread().join() // block forever; shutdown hook stops the bots
}

private fun raw(prop: String, env: String): String? =
    System.getProperty(prop) ?: System.getenv(env)

private fun intOf(prop: String, env: String) = raw(prop, env)?.trim()?.toIntOrNull()
