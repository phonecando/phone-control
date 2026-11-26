package com.phonecando.pce.gates.shots

import org.junit.Assert.assertEquals
import org.junit.Test

// When each screenshot goes: its agent's next one, another agent getting
// the screen, its agent done, too old.
class ShotBookTest {
  private val book = ShotBook(maxAgeMs = 1_000)

  @Test fun anAgentsNextScreenshotReplacesItsLast() {
    assertEquals(emptyList<String>(), book.taken("a1", "s1", 0))
    assertEquals(emptyList<String>(), book.taken("a2", "s2", 0))
    assertEquals(listOf("s1"), book.taken("a1", "s3", 10))
    assertEquals(listOf("s2", "s3"), book.all())
  }

  @Test fun anotherAgentGettingTheScreenTakesTheRest() {
    book.taken("a1", "s1", 0)
    book.taken("a2", "s2", 0)
    assertEquals(listOf("s1"), book.screenTo("a2"))
    assertEquals(emptyList<String>(), book.screenTo("a2"))
    assertEquals(listOf("s2"), book.ended("a2"))
  }

  @Test fun theShotsOnDiskByAgent() {
    book.taken("a1", "/c/s1", 0)
    book.taken("a2", "/c/s2", 0)
    assertEquals(mapOf("a1" to listOf("/c/s1"), "a2" to listOf("/c/s2")), book.byAgent())
    book.ended("a1")
    assertEquals(mapOf("a2" to listOf("/c/s2")), book.byAgent())
  }

  @Test fun oldOnesGo() {
    book.taken("a1", "s1", 0)
    assertEquals(emptyList<String>(), book.expired(999))
    assertEquals(listOf("s1"), book.expired(1_000))
    // Taking one sweeps the old ones too.
    book.taken("a2", "s2", 0)
    assertEquals(listOf("s2"), book.taken("a1", "s3", 5_000))
  }
}
