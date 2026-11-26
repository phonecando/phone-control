package com.phonecando.pce.gates.a11y

// The test tools that open a UiAutomation connection on the phone, by the
// package their on-phone driver installs. While one holds it, Android
// binds no accessibility service (A11yStatusRules), so a leftover driver
// silently keeps ours from starting. The host must be able to see these
// packages (Android 11+ <queries>). Pure.
object TestDrivers {
  /** Package to the tool's name. */
  val PACKAGES: Map<String, String> = linkedMapOf(
    "dev.mobile.maestro" to "Maestro",
    "dev.mobile.maestro.test" to "Maestro",
    "io.appium.uiautomator2.server" to "Appium",
    "io.appium.uiautomator2.server.test" to "Appium",
    "com.github.uiautomator" to "uiautomator2",
    "com.github.uiautomator.test" to "uiautomator2",
    "androidx.test.orchestrator" to "AndroidX Test Orchestrator",
    "androidx.test.services" to "AndroidX Test Services",
  )

  /** The installed ones among [PACKAGES], in its order. */
  fun installed(isInstalled: (pkg: String) -> Boolean): List<String> = PACKAGES.keys.filter(isInstalled)

  /** Each tool once, by name, for [packages]. */
  fun names(packages: List<String>): List<String> = packages.mapNotNull(PACKAGES::get).distinct()
}
