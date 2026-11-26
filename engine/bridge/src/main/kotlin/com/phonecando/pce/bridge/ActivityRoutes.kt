package com.phonecando.pce.bridge

import org.json.JSONObject

// The routes that start an activity: /share, /open and /app. Android blocks
// background activity starts, so /share and /open work while the app is
// foreground (the normal agent flow) and are silently dropped if the user
// backgrounded it mid-run; the host's /app falls back to a notification.
// /open only takes the schemes in ALLOWED_SCHEMES, and the router has
// already judged the app Android hands the link to (gates/appaccess).
// /app is ungated: going back to the app is not phone control; the
// holder's successful /app also hands the screen back (BridgeRouter).
// Pure: starting activities is the host's.
class ActivityRoutes<C : Caller>(private val activities: Activities, private val back: AppReturn<C>) {
  fun table(): Map<BridgeRoute, RouteHandler<C>> = mapOf(
    PhoneRoute.SHARE to { call -> share(call.body) },
    PhoneRoute.OPEN to { call -> open(call.body) },
    PhoneRoute.APP to { call -> app(call.agent, call.body) },
  )

  private fun share(o: JSONObject): Reply {
    val text = o.optString("text", "").takeIf { it.isNotBlank() }
      ?: return Reply.badRequest("bad_request", "text is required")
    val title = o.optString("title", "").takeIf { it.isNotBlank() }
    return try {
      activities.share(text, title)
      Reply.ok(JSONObject().put("started", true))
    } catch (e: Exception) {
      Reply.fail("no_activity", e.message ?: "no app to share to")
    }
  }

  private fun open(o: JSONObject): Reply {
    val raw = o.optString("url", "").takeIf { it.isNotBlank() }
      ?: return Reply.badRequest("bad_request", "url is required")
    val parsed = try {
      activities.scheme(raw)
    } catch (_: Exception) {
      return Reply.badRequest("bad_url", "cannot parse url")
    }
    val scheme = parsed?.lowercase()
      ?: return Reply.badRequest("bad_url", "url needs a scheme")
    if (scheme !in ALLOWED_SCHEMES) {
      return Reply.badRequest("bad_scheme", "allowed: ${ALLOWED_SCHEMES.joinToString(",")}")
    }
    return try {
      activities.view(raw)
      Reply.ok(JSONObject().put("started", true))
    } catch (e: Exception) {
      Reply.fail("no_activity", e.message ?: "nothing handles $scheme")
    }
  }

  private fun app(caller: C, o: JSONObject): Reply {
    val out = back.bringBack(caller, o.optString("tab", "home"), o.optBoolean("notify", false))
      ?: return Reply.fail("no_activity", "cannot resolve PhoneCanDo launch intent")
    return Reply.ok(JSONObject().put("started", out.started).put("notified", out.notified))
  }

  companion object {
    /** The only link schemes /open starts. */
    val ALLOWED_SCHEMES = setOf("http", "https", "mailto", "tel", "geo", "market")
  }
}
