package com.phonecando.pce.bridge

import com.phonecando.pce.gates.a11y.A11yState
import com.phonecando.pce.gates.a11y.A11yStatus
import com.phonecando.pce.gates.controllock.ControlLock
import org.json.JSONObject

// /unlock and /accessibility (full builds only, ungated: both are safe while
// phone control is locked). Each asks the app to show its dialog over the
// chat for this request alone and blocks until the user is done, so the
// agent continues in the same turn (the host's UserQuestions has the
// waits). `restricted`: the host's best guess at whether Android holds the
// service back as a restricted setting ("none", "sideload" or another
// state of its own). `status`: the service's status (gates/a11y) once the
// wait is over, so the reply names its state and the fix for it (a
// service Android won't start, or one that failed, is not fixed in
// Settings); null when the host can't tell.
class PromptRoutes<C : Caller>(
  private val questions: UserQuestions<C>,
  private val automation: () -> AutomationHost?,
  private val lock: ControlLock,
  private val status: () -> A11yStatus? = { null },
  private val restricted: () -> String,
) {
  fun table(): Map<BridgeRoute, RouteHandler<C>> = mapOf(
    PhoneRoute.UNLOCK to { call -> unlock(call) },
    PhoneRoute.ACCESSIBILITY to { call -> accessibility(call) },
  )

  // The app's biometric/PIN modal is the only place the lock opens; the
  // agent never sees or supplies a credential. In ask mode the user's yes
  // is this agent's own unlock (ControlLock.grantAsk).
  private fun unlock(call: BridgeCall<C>): Reply {
    val waitMs = call.body.optLong("waitMs", 45_000).coerceIn(5_000, 90_000)
    val agentId = call.agent.id
    val out = questions.unlock(call.agent, "phone_control", waitMs) { lock.unlockedFor(agentId) }
    if (out.requested && out.unlocked) lock.grantAsk(agentId)
    return Reply.ok(
      JSONObject().put("requested", out.requested).put("unlocked", out.unlocked).put("notified", out.notified),
    )
  }

  // Waits for the accessibility service to connect; the guide's three
  // steps fit in the default wait, which stays under five minutes, where
  // common HTTP clients give up on a response. `restricted` is "sideload"
  // when the user went through the restricted-settings guide, "none" once
  // the service is on, otherwise the host's best guess.
  private fun accessibility(call: BridgeCall<C>): Reply {
    val waitMs = call.body.optLong("waitMs", 240_000).coerceIn(30_000, 280_000)
    val out = questions.accessibility(call.agent, "accessibility", waitMs) {
      automation()?.connected == true
    }
    val state = when {
      out.guided -> SIDELOAD
      out.enabled -> NONE
      else -> restricted()
    }
    val data = JSONObject()
      .put("opened", out.opened)
      .put("enabled", out.enabled)
      .put("cancelled", out.cancelled)
      .put("notified", out.notified)
      .put("restricted", state)
    status()?.let { now ->
      data.put("state", now.state.wire)
      now.cause?.let { data.put("cause", it) }
      if (now.state != A11yState.READY) data.put("fix", A11yFixes.of(now))
    }
    return Reply.ok(data)
  }

  companion object {
    const val SIDELOAD = "sideload"
    const val NONE = "none"
  }
}
