package com.phonecando.pce.gates.a11y

// The service's state from what Android and the service say, first match:
//   no service in this build                      ABSENT
//   the PCE adapter is up                         READY
//   not switched on (or Accessibility is off)     OFF (cause: Auto off, restricted settings)
//   the last start failed                         FAILED (its reason)
//   Android bound it, the adapter not up yet      STARTING
//   switched on, not bound, under GRACE_MS        STARTING
//   ... over it, after it ran this time           STOPPED
//   ... over it, never ran since switched on      NOT_STARTED
// Android binds no accessibility service while a UiAutomation connection
// (Maestro, UI Automator, Appium, an adb instrumentation) is open without
// asking to keep them, and says nothing: the switch stays on. So NOT_STARTED
// and STOPPED name the test tools installed, the usual cause. Pure.
object A11yStatusRules {
  /** How long a switched-on service may take to start before it counts as stuck. */
  const val GRACE_MS = 5_000L

  /**
   * One reading: the build has a service ([hasService]); it is in
   * Android's enabled list with Accessibility on ([enabled]); Android has
   * it bound ([bound]); the PCE adapter is up ([connected]); the last
   * start's [failure], if it failed; it ran since it was last switched on
   * ([ranBefore]); since when it has been switched on but not bound
   * ([unboundSince], null when it is not); Auto off switched it off
   * ([autoOff]); Android may be holding the switch back ([restricted]);
   * the test tools installed ([testDrivers]).
   */
  data class Facts(
    val hasService: Boolean,
    val enabled: Boolean,
    val bound: Boolean,
    val connected: Boolean,
    val failure: Failure? = null,
    val ranBefore: Boolean = false,
    val unboundSince: Long? = null,
    val now: Long = 0,
    val autoOff: Boolean = false,
    val restricted: Boolean = false,
    val testDrivers: List<String> = emptyList(),
  )

  /** Why a start failed, in a short line, and when (elapsedRealtime). */
  data class Failure(val reason: String, val at: Long)

  fun of(f: Facts): A11yStatus {
    if (!f.hasService) return A11yStatus(A11yState.ABSENT)
    if (f.connected) return A11yStatus(A11yState.READY)
    if (!f.enabled) {
      val cause = when {
        f.autoOff -> A11yStatus.AUTO_OFF
        f.restricted -> A11yStatus.RESTRICTED
        else -> null
      }
      return A11yStatus(A11yState.OFF, cause)
    }
    f.failure?.let { return A11yStatus(A11yState.FAILED, detail = it.reason, at = it.at) }
    if (f.bound) return A11yStatus(A11yState.STARTING)
    val since = f.unboundSince ?: f.now
    if (f.now - since < GRACE_MS) return A11yStatus(A11yState.STARTING)
    val state = if (f.ranBefore) A11yState.STOPPED else A11yState.NOT_STARTED
    val cause = if (f.testDrivers.isNotEmpty()) A11yStatus.TEST_DRIVER else null
    return A11yStatus(state, cause, at = since, testDrivers = f.testDrivers)
  }

  /** A start's failure as one short line: the exception's class and message, cut to [max] characters. */
  fun reason(error: Throwable, max: Int = 160): String {
    val name = error.javaClass.simpleName.ifEmpty { "Error" }
    val message = error.message?.lines()?.firstOrNull()?.trim().orEmpty()
    return (if (message.isEmpty()) name else "$name: $message").take(max)
  }
}
