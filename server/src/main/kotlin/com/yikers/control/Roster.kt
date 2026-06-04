package com.yikers.control

// How many human + bot climbers a run spawns. Set once at boot (BootConfig),
// read by the screens. Default = classic single human, so normal play is
// untouched. 0 humans = hands-free (attract / recording) mode.
object Roster {
    var humans = 1
    var bots = 0

    val total get() = humans + bots
    val handsFree get() = humans == 0
}
