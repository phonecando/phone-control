package com.phonecando.pce.bridge

import org.json.JSONObject

/** The phone's clipboard, as the host reaches it. */
interface Clipboard {
  fun set(text: String)

  /** The first item as text; null when there is none or Android hides it (background apps, API 29+). */
  fun get(): String?
}

// /clipboard (gated): set or read the clipboard. Never a screen route: it
// changes nothing on screen. Pure: the clipboard is the host's.
class ClipboardRoutes<C : Caller>(private val clipboard: Clipboard) {
  fun table(): Map<BridgeRoute, RouteHandler<C>> = mapOf(PhoneRoute.CLIPBOARD to { call -> handle(call.body) })

  private fun handle(o: JSONObject): Reply = when (o.optString("action")) {
    "set" -> {
      val text = o.optString("text")
      clipboard.set(text)
      Reply.ok(JSONObject().put("text", text))
    }
    "get" -> {
      val text = clipboard.get()
      // API 29+ returns nothing to background apps; say so instead of "".
      Reply.ok(JSONObject().put("text", text ?: JSONObject.NULL).put("available", !text.isNullOrEmpty()))
    }
    else -> Reply.badRequest("bad_request", "action must be get or set")
  }
}
