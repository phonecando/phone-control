package com.phonecando.pce.gates.a11y

import org.junit.Assert.assertEquals
import org.junit.Test

// The service's life between readings: a failure until the next start, a
// run until it is switched off, and the grace from the first reading that
// sees it switched on but not bound.
class A11yWatchTest {
  private var now = 0L
  private val watch = A11yWatch { now }

  private fun read(enabled: Boolean = true, bound: Boolean = false, connected: Boolean = false) =
    watch.read(hasService = true, enabled = enabled, bound = bound, connected = connected).state

  @Test fun switchedOnButNeverStartedIsNotStartedAfterTheGrace() {
    assertEquals(A11yState.STARTING, read())
    now += A11yStatusRules.GRACE_MS
    assertEquals(A11yState.NOT_STARTED, read())
  }

  @Test fun aServiceThatRanAndWentIsStopped() {
    watch.connected()
    assertEquals(A11yState.READY, read(bound = true, connected = true))
    assertEquals(A11yState.STARTING, read())
    now += A11yStatusRules.GRACE_MS
    assertEquals(A11yState.STOPPED, read())
    // Switched off and on again: it has not run since.
    assertEquals(A11yState.OFF, read(enabled = false))
    assertEquals(A11yState.STARTING, read())
    now += A11yStatusRules.GRACE_MS
    assertEquals(A11yState.NOT_STARTED, read())
  }

  @Test fun aFailureLastsUntilTheNextStartOrTheSwitch() {
    watch.failed(IllegalStateException("no window"))
    assertEquals(A11yState.FAILED, read(bound = true))
    watch.connected()
    assertEquals(A11yState.READY, read(bound = true, connected = true))
    watch.failed(IllegalStateException("again"))
    assertEquals(A11yState.OFF, read(enabled = false))
    assertEquals(A11yState.STARTING, read(bound = true))
  }

  @Test fun bindingEndsTheGraceCount() {
    read()
    now += A11yStatusRules.GRACE_MS
    assertEquals(A11yState.STARTING, read(bound = true))
    now += 1
    assertEquals(A11yState.STARTING, read())
  }
}
