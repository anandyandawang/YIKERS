package com.yikers.ecs.component.augment

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// Pure logic, no GDX/world. Catalog has one augment today, so the cap/swap path is
// unreachable here (an offer always excludes what you own, so you can't be full AND
// offered); it activates once the catalog outgrows MAX_OWNED.
class AugmentOfferBookTest {

    @Test
    fun opensAtMilestoneNotBefore() {
        val book = AugmentOfferBook(step = 50)
        val owned = mutableSetOf<Augment>()

        book.maybeOpen(score = 49, dead = false, slots = listOf(0)) { owned }
        assertFalse(book.isPaused) { "no offer below the milestone" }

        book.maybeOpen(score = 50, dead = false, slots = listOf(0)) { owned }
        assertTrue(book.isPaused) { "offer opens at the milestone" }
        assertEquals(listOf(DoubleJump), book.offerFor(0))
        assertEquals(50, book.nextScore) { "gate holds until the round resolves" }
    }

    @Test
    fun roomStaysFrozenUntilEveryPlayerResolves() {
        val book = AugmentOfferBook(step = 50)
        val owned0 = mutableSetOf<Augment>()
        val owned1 = mutableSetOf<Augment>()
        val ownedBy = mapOf(0 to owned0, 1 to owned1)

        book.maybeOpen(50, dead = false, slots = listOf(0, 1)) { ownedBy.getValue(it) }
        assertTrue(book.isPaused)
        assertEquals(setOf(0, 1), book.activeSlots())

        book.resolve(0, augmentId = "double_jump", swapOutId = null, owned = owned0)
        assertTrue(DoubleJump in owned0) { "slot 0 took its pick" }
        assertTrue(book.isPaused) { "slot 1 still choosing -> room frozen" }
        assertEquals(50, book.nextScore)

        book.resolve(1, augmentId = null, swapOutId = null, owned = owned1) // skip
        assertFalse(book.isPaused) { "all resolved -> resume" }
        assertTrue(owned1.isEmpty()) { "skip grants nothing" }
        assertEquals(100, book.nextScore) { "gate advances one step after the round" }
    }

    @Test
    fun nobodyOfferableAdvancesGateWithoutPausing() {
        val book = AugmentOfferBook(step = 50)
        val owned = mutableSetOf<Augment>(DoubleJump) // owns the whole catalog

        book.maybeOpen(50, dead = false, slots = listOf(0)) { owned }
        assertFalse(book.isPaused) { "nothing left to offer -> no freeze" }
        assertEquals(100, book.nextScore) { "still skip past this milestone" }
    }

    @Test
    fun deadRunNeverOffers() {
        val book = AugmentOfferBook(step = 50)
        book.maybeOpen(50, dead = true, slots = listOf(0)) { mutableSetOf() }
        assertFalse(book.isPaused)
    }

    @Test
    fun leavingMidRoundUnfreezesRoom() {
        val book = AugmentOfferBook(step = 50)
        book.maybeOpen(50, dead = false, slots = listOf(0, 1)) { mutableSetOf() }
        assertTrue(book.isPaused)

        book.drop(0)
        assertTrue(book.isPaused) { "slot 1 still open" }
        book.drop(1)
        assertFalse(book.isPaused) { "last offer gone -> resume" }
        assertEquals(100, book.nextScore)
    }
}
