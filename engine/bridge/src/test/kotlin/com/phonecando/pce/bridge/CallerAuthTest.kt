package com.phonecando.pce.bridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Bearer token -> calling agent, with several agents running.
class CallerAuthTest {
  private val one = agent("a1", "opencode", "tok-one")
  private val two = agent("a2", "opencode 2", "tok-two")
  private val dir = FakeDirectory(one, two)

  @Test fun eachTokenNamesItsAgent() {
    assertEquals(one, CallerAuth.caller("Bearer tok-one", dir.all()))
    assertEquals(two, CallerAuth.caller("Bearer tok-two", dir.all()))
  }

  @Test fun unknownOrMalformedTokensMatchNobody() {
    assertNull(CallerAuth.caller("Bearer tok-three", dir.all()))
    assertNull(CallerAuth.caller("Bearer tok-on", dir.all()))
    assertNull(CallerAuth.caller("tok-one", dir.all()))
    assertNull(CallerAuth.caller("Basic tok-one", dir.all()))
    assertNull(CallerAuth.caller("Bearer ", dir.all()))
    assertNull(CallerAuth.caller(null, dir.all()))
  }

  @Test fun anAgentWithoutATokenNeverMatches() {
    val blank = FakeDirectory(agent("a3", token = ""))
    assertNull(CallerAuth.caller("Bearer ", blank.all()))
    assertNull(CallerAuth.caller("Bearer x", blank.all()))
  }

  @Test fun aStoppedEngineStopsAuthenticating() {
    val live = FakeDirectory(one, two)
    live.records.remove(two)
    assertNull(CallerAuth.caller("Bearer tok-two", live.all()))
    assertEquals(one, CallerAuth.caller("Bearer tok-one", live.all()))
  }
}
