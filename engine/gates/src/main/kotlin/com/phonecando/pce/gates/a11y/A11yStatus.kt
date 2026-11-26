package com.phonecando.pce.gates.a11y

/** Where the accessibility service stands; `wire` is the name the bridge and the app use. */
enum class A11yState(val wire: String) {
  /** This build has no service (no phone control in it). */
  ABSENT("absent"),

  /** Not switched on in Android's Accessibility settings. */
  OFF("off"),

  /** Switched on and not up yet, within the grace Android takes to start it. */
  STARTING("starting"),

  /** Switched on, but Android has not started it after the grace. */
  NOT_STARTED("not_started"),

  /** Android started it, but setting it up failed. */
  FAILED("failed"),

  /** It ran, and Android stopped it while it is still switched on. */
  STOPPED("stopped"),

  /** Up: agents can see and use the phone (once the lock lets them). */
  READY("ready"),
}

/**
 * The service's status: its [state], what most likely caused it ([cause]:
 * AUTO_OFF or RESTRICTED for OFF, TEST_DRIVER for NOT_STARTED and STOPPED;
 * null when nothing is known), for FAILED the short [detail] of what went
 * wrong and [at] when (elapsedRealtime), and the test tools installed on
 * the phone that can hold Android's automation connection ([testDrivers]).
 */
data class A11yStatus(
  val state: A11yState,
  val cause: String? = null,
  val detail: String? = null,
  val at: Long? = null,
  val testDrivers: List<String> = emptyList(),
) {
  /** Switched on and still not working: something to show the user. */
  val stuck: Boolean get() = state == A11yState.NOT_STARTED || state == A11yState.FAILED || state == A11yState.STOPPED

  companion object {
    /** Auto off turned the service off (the app's own switch). */
    const val AUTO_OFF = "auto_off"

    /** Android holds the switch back until the user allows restricted settings for the app. */
    const val RESTRICTED = "restricted"

    /** A test tool installed on the phone can hold Android's automation connection. */
    const val TEST_DRIVER = "test_driver"
  }
}
