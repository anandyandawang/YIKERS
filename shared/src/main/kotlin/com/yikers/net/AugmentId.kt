package com.yikers.net

import kotlinx.serialization.Serializable

// Wire identity + display copy for every augment. The server maps an id to live
// Augment behavior (AugmentCatalog); client/bot only ever see the id.
@Serializable
enum class AugmentId(val displayName: String, val blurb: String) {
    DOUBLE_JUMP("DOUBLE JUMP", "one extra mid-air jump"),
    AIR_JETS("AIR JETS", "two extra mid-air jumps"),
    SWIFT_BOOTS("SWIFT BOOTS", "+30% move speed"),
    LONG_STRIDE("LONG STRIDE", "+15% move speed"),
    MOON_BOOTS("MOON BOOTS", "+25% jump power"),
    SPRING_LEGS("SPRING LEGS", "+12% jump power"),
    ADRENALINE("ADRENALINE", "+10% speed, +8% jump"),
}
