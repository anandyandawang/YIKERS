package com.yikers.ecs

import com.github.quillraven.fleks.Entity

/**
 * Set of entities, keyed internally by [Entity.id].
 *
 * Why not a raw `HashSet<Entity>`? The iOS/RoboVM runtime ships an old libcore
 * with no static `Integer.hashCode(int)` (a Java 8 API). Fleks' generated
 * `Entity.hashCode()` calls it, so any hash-based `HashSet<Entity>` op crashes
 * on device with `NoSuchMethodError`. Boxing a plain `Int` instead uses the
 * instance `Integer.hashCode()`, which RoboVM does have.
 *
 * This wrapper hides the id swap so call sites stay expressive:
 * `set.add(entity)` and `entity in set`.
 */
class EntitySet {
    private val ids = HashSet<Int>()

    fun add(entity: Entity) {
        ids += entity.id
    }

    operator fun contains(entity: Entity): Boolean = entity.id in ids

    fun clear() = ids.clear()

    val size: Int get() = ids.size
}
