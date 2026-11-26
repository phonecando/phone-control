package com.phonecando.pce.bridge

import com.phonecando.pce.gates.a11y.A11yState
import com.phonecando.pce.gates.a11y.A11yStatus

// What an agent is told to do about the accessibility service in each
// state (gates/a11y), in the 503 automation_unavailable's message and
// /accessibility's `fix`. Switching it on in Settings helps only while it
// is off; a service Android won't start, or one that failed, needs
// something else. Pure.
object A11yFixes {
  const val AUTO_OFF = "Auto off turned PhoneCanDo's accessibility service off while no agent used the screen, so " +
    "payment apps open; call phone_open_accessibility to ask the user to turn it back on"

  /** The 503's message as it always read for a service that is simply off. */
  const val OFF = "enable PhoneCanDo in Android accessibility settings"

  fun of(status: A11yStatus): String = when (status.state) {
    A11yState.ABSENT -> "this PhoneCanDo build has no accessibility service, so phone control isn't available in it"
    A11yState.OFF -> when (status.cause) {
      A11yStatus.AUTO_OFF -> AUTO_OFF
      A11yStatus.RESTRICTED -> "PhoneCanDo's accessibility service is off, and Android holds the switch back until the " +
        "user allows restricted settings for PhoneCanDo; call phone_open_accessibility, which walks them through it"
      else -> "PhoneCanDo's accessibility service is off; call phone_open_accessibility to ask the user to turn it on"
    }
    A11yState.STARTING -> "PhoneCanDo's accessibility service is starting; try again in a few seconds"
    A11yState.NOT_STARTED -> "PhoneCanDo's accessibility service is switched on, but Android has not started it. " +
      stuckCause(status)
    A11yState.STOPPED -> "PhoneCanDo's accessibility service was running and Android stopped it, though it is still " +
      "switched on. " + stuckCause(status)
    A11yState.FAILED -> "PhoneCanDo's accessibility service started but failed to set up" +
      (status.detail?.let { " ($it)" } ?: "") + ". Ask the user to switch it off and on again in Android's " +
      "Accessibility settings, and to report it from PhoneCanDo if it keeps failing"
    A11yState.READY -> "PhoneCanDo's accessibility service is running"
  }

  // Android binds no accessibility service while a test tool holds its
  // automation connection; the switch stays on, so Settings can't fix it.
  private fun stuckCause(status: A11yStatus): String {
    val tools = if (status.testDrivers.isEmpty()) "" else " Installed: ${status.testDrivers.joinToString(", ")}."
    return "Usually a test tool (Maestro, UI Automator, Appium or an adb instrumentation) is holding the phone's " +
      "automation connection.$tools Ask the user to stop it, or to restart the phone; opening Accessibility " +
      "settings won't help"
  }
}
