package com.yikers.bot.app

import com.yikers.config.Knobs
import com.yikers.net.discovery.DEFAULT_TCP_PORT

// Connects bot clients to a server. ./gradlew :bot:run (YIKERS_HOST/PORT/BOTS).
fun main() {
    val host = Knobs.string("yikers.host", "YIKERS_HOST") ?: "127.0.0.1"
    val port = Knobs.int("yikers.port", "YIKERS_PORT") ?: DEFAULT_TCP_PORT
    val count = (Knobs.int("yikers.bots", "YIKERS_BOTS") ?: 1).coerceAtLeast(1)

    val runner = BotRunner(host, port, count)
    runner.start()
    println("YIKERS bots: $count connected to $host:$port")

    Runtime.getRuntime().addShutdownHook(Thread { runner.stop() })
    Thread.currentThread().join() // block forever; shutdown hook stops the bots
}
