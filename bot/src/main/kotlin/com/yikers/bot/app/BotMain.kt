package com.yikers.bot.app

import com.yikers.net.discovery.DEFAULT_TCP_PORT

// Connects bot clients to a running server. Pure JVM (a bot is just a client).
//   ./gradlew :bot:run   (env: YIKERS_HOST, YIKERS_PORT, YIKERS_BOTS)
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
