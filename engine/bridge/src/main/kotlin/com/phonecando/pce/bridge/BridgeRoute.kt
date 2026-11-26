package com.phonecando.pce.bridge

/**
 * One bridge route and what guards it, in the order the router checks
 * (PhoneRoute has the phone's; the host adds routes of its own that are
 * none of these):
 *   fullOnly: exists only in a build with phone control; one without answers 404 not_found.
 *   gated:    needs the phone-control lock open for the caller (403 automation_off).
 *   pce:      needs the accessibility service connected (503 automation_unavailable).
 *   screen:   changes what is on screen, or replaces the PCE's one current
 *             snapshot, so it takes the screen lease (409 control_busy or
 *             screen_refused), and is turned away while you have the
 *             controls (409 user_has_controls or paused_by_user).
 */
interface BridgeRoute {
  val method: String
  val path: String
  val fullOnly: Boolean
  val gated: Boolean
  val pce: Boolean
  val screen: Boolean
}
