package com.yikers.config

// One way to read launch knobs: -Dyikers.foo system property beats YIKERS_FOO env.
object Knobs {
    fun string(prop: String, env: String): String? =
        System.getProperty(prop) ?: System.getenv(env)

    fun int(prop: String, env: String): Int? = string(prop, env)?.trim()?.toIntOrNull()

    fun long(prop: String, env: String): Long? = string(prop, env)?.trim()?.toLongOrNull()
}
