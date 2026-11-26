package com.phonecando.pce.gates.a11y

// What the service's own life says between two readings, for
// A11yStatusRules: whether its last start failed, whether it ran since it
// was last switched on, and since when it has been switched on but not
// bound. The service tells it each start, failure and stop; each reading
// passes Android's side in. Pure: the clock is injected.
class A11yWatch(private val clock: () -> Long) {
  private var failure: A11yStatusRules.Failure? = null
  private var ran = false
  private var unboundSince: Long? = null

  /** The adapter came up: any earlier failure is over. */
  @Synchronized
  fun connected() {
    failure = null
    ran = true
    unboundSince = null
  }

  /** Setting the service up threw [error]. */
  @Synchronized
  fun failed(error: Throwable) {
    failure = A11yStatusRules.Failure(A11yStatusRules.reason(error), clock())
  }

  /** The status now, from Android's side ([hasService], [enabled], [bound], [connected]) and the app's. */
  @Synchronized
  fun read(
    hasService: Boolean,
    enabled: Boolean,
    bound: Boolean,
    connected: Boolean,
    autoOff: Boolean = false,
    restricted: Boolean = false,
    testDrivers: List<String> = emptyList(),
  ): A11yStatus {
    val now = clock()
    if (!enabled) {
      // Switched off: what went before no longer says anything.
      failure = null
      ran = false
    }
    unboundSince = if (enabled && !bound && !connected) unboundSince ?: now else null
    return A11yStatusRules.of(
      A11yStatusRules.Facts(
        hasService = hasService,
        enabled = enabled,
        bound = bound,
        connected = connected,
        failure = failure,
        ranBefore = ran,
        unboundSince = unboundSince,
        now = now,
        autoOff = autoOff,
        restricted = restricted,
        testDrivers = testDrivers,
      ),
    )
  }
}
