package com.phonecando.pce.gates.a11y

import com.phonecando.pce.gates.a11y.A11yStatusRules.Facts
import com.phonecando.pce.gates.a11y.A11yStatusRules.Failure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Each state from what Android and the service say, in the rules' order.
class A11yStatusRulesTest {
  private val on = Facts(hasService = true, enabled = true, bound = false, connected = false, now = 10_000)

  private fun state(f: Facts) = A11yStatusRules.of(f).state

  @Test fun noServiceIsAbsentWhateverElse() {
    assertEquals(A11yState.ABSENT, state(on.copy(hasService = false, connected = true)))
  }

  @Test fun anAdapterUpIsReadyEvenBeforeAndroidListsIt() {
    assertEquals(A11yState.READY, state(on.copy(connected = true)))
    assertFalse(A11yStatusRules.of(on.copy(connected = true)).stuck)
  }

  @Test fun offSaysWhyWhenItKnows() {
    val off = on.copy(enabled = false)
    assertEquals(A11yStatus(A11yState.OFF), A11yStatusRules.of(off))
    assertEquals(A11yStatus.AUTO_OFF, A11yStatusRules.of(off.copy(autoOff = true, restricted = true)).cause)
    assertEquals(A11yStatus.RESTRICTED, A11yStatusRules.of(off.copy(restricted = true)).cause)
  }

  @Test fun aFailedStartSaysWhatWentWrong() {
    val failed = A11yStatusRules.of(on.copy(bound = true, failure = Failure("IllegalStateException: no window", 7)))
    assertEquals(A11yState.FAILED, failed.state)
    assertEquals("IllegalStateException: no window", failed.detail)
    assertEquals(7L, failed.at)
    assertTrue(failed.stuck)
  }

  @Test fun boundWithoutTheAdapterIsStarting() {
    assertEquals(A11yState.STARTING, state(on.copy(bound = true)))
  }

  @Test fun notBoundGetsAGraceThenIsStuck() {
    assertEquals(A11yState.STARTING, state(on.copy(unboundSince = 10_000 - A11yStatusRules.GRACE_MS + 1)))
    assertEquals(A11yState.STARTING, state(on.copy(unboundSince = null)))
    val stuck = A11yStatusRules.of(on.copy(unboundSince = 10_000 - A11yStatusRules.GRACE_MS))
    assertEquals(A11yState.NOT_STARTED, stuck.state)
    assertNull(stuck.cause)
    assertEquals(A11yState.STOPPED, state(on.copy(unboundSince = 0, ranBefore = true)))
  }

  @Test fun aStuckServiceNamesTheTestToolsInstalled() {
    val s = A11yStatusRules.of(on.copy(unboundSince = 0, testDrivers = listOf("dev.mobile.maestro")))
    assertEquals(A11yStatus.TEST_DRIVER, s.cause)
    assertEquals(listOf("dev.mobile.maestro"), s.testDrivers)
  }

  @Test fun aReasonIsOneShortLine() {
    assertEquals("IllegalStateException: boom", A11yStatusRules.reason(IllegalStateException("boom\nat x")))
    assertEquals("NullPointerException", A11yStatusRules.reason(NullPointerException()))
    assertEquals(20, A11yStatusRules.reason(RuntimeException("x".repeat(500)), max = 20).length)
  }
}
