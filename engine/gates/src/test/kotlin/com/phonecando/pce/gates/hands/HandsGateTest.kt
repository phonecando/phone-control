package com.phonecando.pce.gates.hands

import com.phonecando.pce.gates.hands.HandsGate.Refusal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// The gate per kind of route, mode and agent: a route that leaves the
// screen alone, one that only looks at it (/pce/observe), one that drives it.
class HandsGateTest {
  private val watch = HandOver("a1", "opencode", HandMode.WATCH, HandBy.USER, null, 0)
  private val paused = watch.copy(mode = HandMode.PAUSED)

  private fun off(agentId: String, on: HandOver?, left: Boolean = false) = HandsGate.check(agentId, false, false, on, left)

  private fun look(agentId: String, on: HandOver?, left: Boolean = false) = HandsGate.check(agentId, true, true, on, left)

  private fun drive(agentId: String, on: HandOver?, left: Boolean = false) = HandsGate.check(agentId, true, false, on, left)

  @Test fun noHandOverPassesEverything() {
    assertNull(off("a1", null))
    assertNull(look("a1", null))
    assertNull(drive("a1", null))
  }

  @Test fun routesThatLeaveTheScreenAlwaysPass() {
    assertNull(off("a1", paused))
    assertNull(off("a2", watch))
    assertNull(off("a1", watch))
  }

  @Test fun theWatchingAgentMayOnlyLook() {
    assertNull(look("a1", watch))
    assertEquals(Refusal.USER_HAS_CONTROLS, drive("a1", watch))
  }

  @Test fun thePausedAgentMayDoNothing() {
    assertEquals(Refusal.PAUSED_BY_USER, look("a1", paused))
    assertEquals(Refusal.PAUSED_BY_USER, drive("a1", paused))
  }

  @Test fun anAgentLeftStoppedStaysOffTheScreenWithNoHandOver() {
    assertEquals(Refusal.PAUSED_BY_USER, look("a1", null, left = true))
    assertEquals(Refusal.PAUSED_BY_USER, drive("a1", null, left = true))
    assertNull(off("a1", null, left = true))
  }

  @Test fun anyOtherAgentIsTurnedAway() {
    for (h in listOf(watch, paused)) {
      assertEquals(Refusal.USER_HAS_CONTROLS, look("a2", h))
      assertEquals(Refusal.USER_HAS_CONTROLS, drive("a2", h))
    }
  }
}
