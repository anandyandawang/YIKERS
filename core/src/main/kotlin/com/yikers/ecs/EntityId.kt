package com.yikers.ecs

/**
 * A Fleks [com.github.quillraven.fleks.Entity]'s raw id.
 *
 * Used as a hash key in place of `Entity` itself: the iOS/RoboVM runtime ships
 * an old libcore with no static `Integer.hashCode(int)` (a Java 8 API). Fleks'
 * generated `Entity.hashCode()` calls it, so a `HashSet<Entity>` crashes on
 * device. Boxing a plain `Int` uses the instance `Integer.hashCode()`, which
 * RoboVM does have. The alias keeps intent readable.
 */
typealias EntityId = Int
