package com.phonecando.pce.bridge

import com.phonecando.pce.gates.hands.HandMode
import com.phonecando.pce.gates.hands.HandOver
import com.phonecando.pce.gates.hands.HandWait
import com.phonecando.pce.gates.hands.HandsText
import org.json.JSONObject

/** The host's hand-over store, which runs HandsRules and wakes waiting calls. */
interface HandsDesk {
  /** [agentId] hands you the controls (WATCH, or PAUSED when [private]); null while one is already on. */
  fun agent(agentId: String, agentName: String, why: String, private: Boolean): HandOver?

  /** Blocks up to [timeoutMs] while [agentId]'s hand-over is on in WATCH. */
  fun await(agentId: String, timeoutMs: Long): HandWait
}

// The agent's side of a hand-over (gates/hands): handing you the controls
// and waiting for them back.
//   /hands/over {why, private?}   WATCH: blocks until you hand them back or
//                                 WAIT_MAX_MS, {mode:"watch", back}; private:
//                                 at once, {mode:"private", back:false, message}
//   /hands/wait {timeoutSec?}     {back, mode?}: blocks while you have the
//                                 controls in WATCH (Let me), until you hand
//                                 them back or its time is up; back true at
//                                 once when none is the caller's; stopped
//                                 (Stop), at once with message: end the turn
// Let me is a pause: a wait that runs out says to call it again (one call
// per WAIT_MAX_MS, the same turn). Stop really stops: the note sends the
// agent on when you start it again. Both are full only and gated, and
// /hands/over is a screen route (the caller holds the screen or takes a
// free one; one already on is 409 user_has_controls in the router). A wait
// blocks on the request's own thread. A missing or mistyped argument is
// 400 bad_request. Pure: the desk is injected.
class HandsRoutes<C : Caller>(private val desk: HandsDesk, private val waitMaxMs: Long = WAIT_MAX_MS) {
  fun table(): Map<BridgeRoute, RouteHandler<C>> = mapOf(
    PhoneRoute.HANDS_OVER to ::handOver,
    PhoneRoute.HANDS_WAIT to ::waitForUser,
  )

  private fun handOver(call: BridgeCall<C>): Reply {
    val why = when (val w = call.body.opt("why")) {
      is String -> w.replace(LINE_BREAKS, " ").trim()
      null, JSONObject.NULL -> ""
      else -> return wrongType("why")
    }
    if (why.isEmpty()) return Reply.badRequest(BAD, "why is required: one short line the user sees, why you need them")
    if (why.length > WHY_MAX) return Reply.badRequest(BAD, "why is ${why.length} characters, over $WHY_MAX")
    val private = when (val p = call.body.opt("private")) {
      is Boolean -> p
      null, JSONObject.NULL -> false
      else -> return wrongType("private")
    }
    val started = desk.agent(call.agent.id, call.agent.name, why, private)
      ?: return Reply.userHasControls(HandsText.USER_HAS_CONTROLS)
    if (started.mode == HandMode.PAUSED) return Reply.ok(paused(HandsText.HANDED_PRIVATE))
    // A watch hand-over waits here for you, as a first /hands/wait would.
    return when (desk.await(call.agent.id, waitMaxMs)) {
      HandWait.BACK -> Reply.ok(JSONObject().put("mode", "watch").put("back", true))
      HandWait.WATCH -> Reply.ok(stillYours().put("mode", "watch"))
      HandWait.PAUSED -> Reply.ok(paused(HandsText.PAUSED))
    }
  }

  private fun waitForUser(call: BridgeCall<C>): Reply {
    val ms = when (val t = call.body.opt("timeoutSec")) {
      null, JSONObject.NULL -> waitMaxMs
      is Number -> t.toDouble().takeIf { it.isFinite() }?.let { (it * 1_000).toLong().coerceIn(1_000, maxOf(waitMaxMs, 1_000)) }
        ?: return wrongType("timeoutSec")
      else -> return wrongType("timeoutSec")
    }
    return when (desk.await(call.agent.id, ms)) {
      HandWait.BACK -> Reply.ok(JSONObject().put("back", true))
      HandWait.WATCH -> Reply.ok(stillYours().put("mode", "watch"))
      HandWait.PAUSED -> Reply.ok(paused(HandsText.PAUSED))
    }
  }

  private fun stillYours() = JSONObject().put("back", false).put("message", HandsText.STILL_YOURS)

  private fun paused(message: String) = JSONObject().put("mode", "private").put("back", false).put("message", message)

  private fun wrongType(key: String) = Reply.badRequest(BAD, "$key has the wrong type")

  companion object {
    /**
     * The longest a hand-over call waits for you: long enough that an agent
     * waits for you in one call, and under the MCP clients' tool timeouts
     * (some cut a call off at five minutes).
     */
    const val WAIT_MAX_MS = 240_000L
    const val WHY_MAX = 120
    private const val BAD = "bad_request"
    private val LINE_BREAKS = Regex("[\\r\\n\\t]+")
  }
}
