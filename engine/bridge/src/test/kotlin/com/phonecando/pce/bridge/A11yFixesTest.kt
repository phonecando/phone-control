package com.phonecando.pce.bridge

import com.phonecando.pce.gates.a11y.A11yState
import com.phonecando.pce.gates.a11y.A11yStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// The fix an agent hears per state: Settings only while the service is off.
class A11yFixesTest {
  private fun fix(state: A11yState, cause: String? = null) = A11yFixes.of(A11yStatus(state, cause))

  @Test fun onlyAnOffServiceSendsTheUserToSettings() {
    assertTrue(fix(A11yState.OFF).contains("phone_open_accessibility"))
    assertTrue(fix(A11yState.OFF, A11yStatus.RESTRICTED).contains("restricted settings"))
    assertTrue(fix(A11yState.OFF, A11yStatus.AUTO_OFF).startsWith("Auto off"))
    for (state in listOf(A11yState.NOT_STARTED, A11yState.STOPPED)) {
      assertTrue(fix(state).contains("won't help"))
      assertFalse(fix(state).contains("phone_open_accessibility"))
    }
    assertTrue(fix(A11yState.FAILED).contains("switch it off and on"))
    assertTrue(fix(A11yState.STARTING).contains("try again"))
  }

  @Test fun everyStateHasAFix() {
    for (state in A11yState.entries) assertTrue(state.wire, fix(state).isNotBlank())
  }
}
