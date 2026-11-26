package com.phonecando.pce.service

import android.content.res.Configuration
import com.phonecando.pce.core.Pce

/**
 * What the host app adds to the accessibility service (PceAccessibilityService),
 * in the order the service calls it. None of it changes what an agent may
 * do: every look and action still goes through the gated backend.
 */
interface ServiceHooks {
  /** Android bound the service; runs before the PCE adapter is made, and a failure here is the host's. */
  fun connected(service: PceAccessibilityService) {}

  /** Starts what the host shows while agents drive; returns what keeps its overlay out of their way. */
  fun start(service: PceAccessibilityService): OverlayGuard = OverlayGuard.NONE

  /** The host's packs, run by [pce]. */
  fun packs(service: PceAccessibilityService, pce: Pce): Packs = Packs.NONE

  /** The adapter is up: the host's bridge may hand it calls now. */
  fun adapterReady(adapter: PceAutomation) {}

  /**
   * Setting the adapter up threw [error] (the host's [start] included):
   * the service is bound but agents can't use it. The host keeps it to
   * say so (gates/a11y's FAILED).
   */
  fun failed(service: PceAccessibilityService, error: Throwable) {}

  /** A screenshot [agentId] was shown, at [path]: the host decides how long it stays. */
  fun shotTaken(agentId: String, path: String) {}

  /** The phone's light or dark (or another configuration) changed. */
  fun configurationChanged(config: Configuration) {}

  /** The service is going; [adapter] (null if it never came up) must stop taking calls. */
  fun stopped(adapter: PceAutomation?) {}

  companion object {
    /** Adds nothing. */
    val NONE: ServiceHooks = object : ServiceHooks {}
  }
}
