package com.phonecando.pce.gates.hands

// What a screen request gets while you have the controls (the bridge's
// router, after the lock and service checks, before the lease):
//   an agent you left stopped         every screen route: paused_by_user, until the run it was stopped in ends
//   the hand-over's agent, PAUSED     every screen route: paused_by_user (end the turn; a repeat stops it)
//   the hand-over's agent, WATCH      a look (/pce/observe) passes (it may follow along); any other: user_has_controls
//   any other agent                   every screen route: user_has_controls
// Routes that leave the screen alone, /hands/wait among them, always pass.
// Pure.
object HandsGate {
  enum class Refusal { USER_HAS_CONTROLS, PAUSED_BY_USER }

  /**
   * [screen]: the route uses the screen; [look]: it only looks at it
   * (/pce/observe). [left]: you left [agentId] stopped and its run still
   * goes on.
   */
  fun check(agentId: String, screen: Boolean, look: Boolean, handOver: HandOver?, left: Boolean = false): Refusal? {
    if (!screen) return null
    if (left) return Refusal.PAUSED_BY_USER
    val on = handOver ?: return null
    if (on.agentId != agentId) return Refusal.USER_HAS_CONTROLS
    return when (on.mode) {
      HandMode.PAUSED -> Refusal.PAUSED_BY_USER
      HandMode.WATCH -> if (look) null else Refusal.USER_HAS_CONTROLS
    }
  }
}
