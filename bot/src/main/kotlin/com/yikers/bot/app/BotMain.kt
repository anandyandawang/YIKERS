package com.yikers.bot.app

import com.yikers.config.Knobs
import com.yikers.net.discovery.DEFAULT_TCP_PORT

// Connects ONE bot to a server: one :bot run == one bot joining. ./gradlew :bot:run
// (YIKERS_HOST/PORT). Want more bots? Launch more :bot processes.
fun main() {
    val host = Knobs.string("yikers.host", "YIKERS_HOST") ?: "127.0.0.1"
    val port = Knobs.int("yikers.port", "YIKERS_PORT") ?: DEFAULT_TCP_PORT

    val bot = BotClient(host, port)
    bot.start()
    println("YIKERS bot connected to $host:$port")

    Runtime.getRuntime().addShutdownHook(Thread { bot.stop() })
    Thread.currentThread().join() // block forever; shutdown hook stops the bot
}
