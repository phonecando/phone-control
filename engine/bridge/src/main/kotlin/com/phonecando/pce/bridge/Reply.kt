package com.phonecando.pce.bridge

import com.phonecando.pce.gates.a11y.A11yState
import com.phonecando.pce.gates.a11y.A11yStatus
import org.json.JSONObject

// One bridge response: an HTTP status and the envelope
// {"ok":true,"data":...} / {"ok":false,"code":...,"message":...}. App-level
// failures (no app to share to, a PCE error) are 200 + ok:false; the status
// is for the bridge's own refusals. The host may answer routes of its own
// in other shapes (an empty body for 202, `Allow` on 405). Pure.
data class Reply(val status: Int, val body: String, val headers: Map<String, String> = emptyMap()) {
  companion object {
    fun ok(data: JSONObject) = Reply(200, JSONObject().put("ok", true).put("data", data).toString())

    fun error(status: Int, code: String, message: String) = Reply(status, errorBody(code, message).toString())

    /** An app-level failure: the call ran and did not work. */
    fun fail(code: String, message: String) = error(200, code, message)

    fun badRequest(code: String, message: String) = error(400, code, message)

    fun notFound(method: String, path: String) = error(404, "not_found", "no route $method $path")

    /** A full-only route on lite/play. */
    fun notInBuild(path: String) = error(404, "not_found", "no route for $path in this build")

    fun automationOff() = error(403, "automation_off", "phone control is locked; unlock it in PhoneCanDo settings")

    /**
     * The accessibility service isn't up. `autoOff`: the app's Auto off
     * turned it off, which the agent hears so it can tell the user.
     * [status], when the host can tell: `reason` names its state
     * (gates/a11y) and `cause` what most likely led to it, and the message
     * says the fix for that state (A11yFixes); an off service reads as it
     * always did.
     */
    fun automationUnavailable(autoOff: Boolean = false, status: A11yStatus? = null): Reply {
      val known = status?.takeIf { it.state != A11yState.READY }
      val message = when {
        known == null -> if (autoOff) A11yFixes.AUTO_OFF else A11yFixes.OFF
        known.state == A11yState.OFF && known.cause == null -> if (autoOff) A11yFixes.AUTO_OFF else A11yFixes.OFF
        else -> A11yFixes.of(known)
      }
      val body = errorBody("automation_unavailable", message)
      if (known != null) {
        body.put("reason", known.state.wire)
        known.cause?.let { body.put("cause", it) }
      }
      return Reply(503, body.toString())
    }

    /** Another agent has the screen (ScreenLease): the caller waits in line at `place`. */
    fun busy(holder: String, retryAfterMs: Long, place: Int) = Reply(
      409,
      errorBody("control_busy", "$holder holds the screen; you are #$place in line; we'll message you when it is yours")
        .put("holder", holder)
        .put("retryAfterMs", retryAfterMs)
        .put("place", place)
        .toString(),
    )

    /** A turn another agent's message started asked for the screen (ScreenLease). */
    fun screenRefused() = error(
      409,
      "screen_refused",
      "a turn another agent's message started can't take the screen; the user or your own earlier request in the queue can",
    )

    /** You have the phone's controls (gates/hands/HandsGate): [message] says what the caller can do meanwhile. */
    fun userHasControls(message: String) = error(409, "user_has_controls", message)

    /** You stopped the caller to use the phone (gates/hands/HandsGate): it ends its turn. */
    fun pausedByUser(message: String) = error(409, "paused_by_user", message)

    // The app in front is one the user has not let agents use yet
    // (gates/appaccess): the agent asks through phone_ask_app, with the package
    // named in the message, as the tools show the model only code and message.
    fun appNotAllowed(pkg: String, label: String) = appRefusal(
      "app_not_allowed",
      "PhoneCanDo asks the user before agents use $label. Call phone_ask_app with package \"$pkg\" and wait for " +
        "the answer. Never work around it.",
      pkg,
      label,
    )

    /** The user said No to agents in this app; Allow all does not lift it. */
    fun appBlocked(pkg: String, label: String) = appRefusal(
      "app_blocked",
      "The user blocked agents from $label. Don't use it; if you need it, call phone_hand_over and ask the user.",
      pkg,
      label,
    )

    // A tap on a notification whose app no installed app's name matches
    // (gates/appaccess/ForeignParts), so nobody can be asked about it. The name is
    // screen text: one line, cut short, in quotes.
    fun notificationHidden(label: String): Reply {
      val name = label.lines().first().take(40)
      return Reply(
        403,
        errorBody(
          "notification_hidden",
          "That notification is from \"$name\", an app PhoneCanDo can't name, so agents can't use it. " +
            "If you need it, call phone_hand_over and ask the user.",
        ).put("label", name).toString(),
      )
    }

    /** A question waits in PhoneCanDo and it is in front (gates/appaccess/OwnQuestions): only the user answers it. */
    fun userDeciding() = error(
      409,
      "user_deciding",
      "The user is answering a question in PhoneCanDo. Wait for it; never tap PhoneCanDo's own questions.",
    )

    private fun appRefusal(code: String, message: String, pkg: String, label: String) = Reply(
      403,
      errorBody(code, message).put("package", pkg).put("label", label).toString(),
    )

    fun errorBody(code: String, message: String): JSONObject =
      JSONObject().put("ok", false).put("code", code).put("message", message)
  }
}
