package com.yikers.control

// How many LOCAL clients a singleplayer run launches: `humans` human clients +
// `bots` in-process bot clients, all joining one embedded room. Set once at boot
// (BootConfig), read by the client screens. This is a launch knob, NOT part of
// SessionConfig — the server has no humans/bots count; it just sees clients.
// Default = classic single human. 0 humans = hands-free (attract / recording).
object Roster {
    var humans = 1
    var bots = 0

    val total get() = humans + bots
    val handsFree get() = humans == 0  // Attract mode
}
