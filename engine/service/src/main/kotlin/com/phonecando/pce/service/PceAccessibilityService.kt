package com.phonecando.pce.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo

// The accessibility service's control part. All observation and action
// logic lives in the PCE adapter (PceAutomationImpl); this class owns the
// Android lifecycle, what the adapter asks of the service (the app in
// front, the app windows on screen, the last activity, an event count),
// and hands the rest to the host (hooks: its overlay, its packs, and
// anything else it runs with the service). The host's subclass is the
// service Android binds, so its class name is the one the user enabled.
abstract class PceAccessibilityService : AccessibilityService() {
  private var hooks: ServiceHooks = ServiceHooks.NONE
  private var pce: PceAutomation? = null

  /** What the host adds; asked once each time Android binds the service. */
  protected abstract fun hooks(): ServiceHooks

  // Android exposes no direct "current activity" query to an accessibility
  // service. Track the last window-state event; the PCE asks for it by
  // package so an IME/dialog event cannot leak into another app's snapshot.
  @Volatile private var activityPackage: String? = null
  @Volatile private var activityClass: String? = null

  // Monotonic accessibility-event counter. phone_wait uses it to notice a
  // change that appeared and reverted between two snapshot polls.
  @Volatile var eventSeq: Long = 0L
    private set

  fun activityName(packageName: String?): String? {
    if (packageName.isNullOrEmpty() || activityPackage != packageName) return null
    return activityClass
  }

  // The app in front, for the app-access gate: the active window's
  // package, else the last activity we saw. The host's overlay must never
  // take focus, so it is never the active window.
  fun frontPackage(): String? =
    runCatching { rootInActiveWindow?.packageName?.toString() }.getOrNull()?.takeIf { it.isNotEmpty() }
      ?: activityPackage

  // The apps with a window on screen, for a screenshot's app check (each
  // half of a split screen, a picture-in-picture): application windows
  // only, as the bars, the keyboard and the host's overlay show no app's
  // screen.
  fun appWindowPackages(): List<String?> = runCatching {
    windows.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
      .map { window -> window.root?.packageName?.toString() }
  }.getOrDefault(emptyList())

  override fun onServiceConnected() {
    super.onServiceConnected()
    val hooks = hooks().also { this.hooks = it }
    hooks.connected(this)
    try {
      val overlay = hooks.start(this)
      val next = PceAutomationImpl(this, overlay, hooks)
      hooks.adapterReady(next)
      pce = next
      Log.i(TAG, "pce adapter connected")
    } catch (t: Throwable) {
      Log.e(TAG, "pce adapter failed", t)
      runCatching { hooks.failed(this, t) }
    }
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null) return
    eventSeq++
    if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
    val pkg = event.packageName?.toString() ?: return
    val cls = event.className?.toString() ?: return
    // Generic root views and system dialogs are not activities.
    if (!isActivity(pkg, cls)) return
    activityPackage = pkg
    activityClass = cls
  }

  // An activity's class need not live under its package name: a dev build
  // (com.phonecando.app.full.dev) runs com.phonecando.app.MainActivity, and
  // many apps ship activities from another namespace. Ask the package manager;
  // only for a package Android 11+ hides from us (outside the manifest's
  // launcher <queries>) fall back to the class-name prefix.
  @Suppress("DEPRECATION")
  private fun isActivity(pkg: String, cls: String): Boolean =
    try {
      packageManager.getActivityInfo(ComponentName(pkg, cls), 0)
      true
    } catch (e: PackageManager.NameNotFoundException) {
      !isVisible(pkg) && cls.startsWith(pkg)
    }

  @Suppress("DEPRECATION")
  private fun isVisible(pkg: String): Boolean =
    try {
      packageManager.getPackageInfo(pkg, 0)
      true
    } catch (e: PackageManager.NameNotFoundException) {
      false
    }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    hooks.configurationChanged(newConfig)
  }

  override fun onInterrupt() {
    Log.w(TAG, "accessibility service interrupted")
  }

  override fun onDestroy() {
    hooks.stopped(pce)
    pce = null
    super.onDestroy()
  }

  private companion object {
    const val TAG = "PhoneCanDoUiAuto"
  }
}
