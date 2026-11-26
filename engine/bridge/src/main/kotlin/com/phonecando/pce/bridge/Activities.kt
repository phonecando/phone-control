package com.phonecando.pce.bridge

/**
 * How the host starts an activity for /share and /open. Each throws when
 * Android has nothing to start (the route answers no_activity with the
 * exception's message).
 */
interface Activities {
  /** The link's scheme as Android parses it; null when it has none. Throws when it can't be parsed. */
  fun scheme(url: String): String?

  /** Opens [url] in the app Android hands it to. */
  fun view(url: String)

  /** The share sheet for [text], with [title] as its subject when there is one. */
  fun share(text: String, title: String?)
}

/**
 * How the host brings itself forward for /app (phone_return), on [tab]
 * with [caller] active. Not phone control: it only shows the app.
 */
fun interface AppReturn<C : Caller> {
  /** Null when the app can't be launched at all; else whether it started, and whether a tap-to-return notice went out. */
  fun bringBack(caller: C, tab: String, notify: Boolean): Back?

  data class Back(val started: Boolean, val notified: Boolean)
}
