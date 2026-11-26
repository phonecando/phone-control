package com.phonecando.pce.bridge

// The routes that reach the phone, and what guards each (BridgeRoute).
// The PCE's screen routes also need the app in front to be one the user
// let agents use (gates/appaccess: 403 app_not_allowed or app_blocked);
// /app-access (gated, full only, not a screen route) asks the user about
// one app and waits for the answer.
// Screen routes: /pce/observe, /pce/act and /pce/pack (a pack run drives the
// screen and every pack call shares the PCE lane), /open and /share (they
// start an activity over whatever is showing) and /app (brings the app
// forward; the holder's /app also ends its lease, see BridgeRouter) and
// /hands/over (an agent handing you the controls: it must hold the screen,
// or take a free one). Not screen routes: /hands/wait (waiting for you to
// hand them back), /clipboard, /pce/describe, and the prompts /unlock and
// /accessibility: they only matter while the gate is off or the service
// is down, when nobody can drive the screen anyway, and the app queues
// their dialogs. In ask mode others can drive while one agent is locked,
// so a locked agent's /unlock waits in line while another agent has the
// screen, and its dialog stops its time on it (BridgeRouter, ScreenLease).
// Pure.
enum class PhoneRoute(
  override val method: String,
  override val path: String,
  override val fullOnly: Boolean = false,
  override val gated: Boolean = false,
  override val pce: Boolean = false,
  override val screen: Boolean = false,
) : BridgeRoute {
  CLIPBOARD("POST", "/clipboard", gated = true),
  SHARE("POST", "/share", gated = true, screen = true),
  OPEN("POST", "/open", gated = true, screen = true),
  APP("POST", "/app", screen = true),
  UNLOCK("POST", "/unlock", fullOnly = true),
  ACCESSIBILITY("POST", "/accessibility", fullOnly = true),
  APP_ACCESS("POST", "/app-access", fullOnly = true, gated = true),
  PCE_DESCRIBE("GET", "/pce/describe", fullOnly = true, gated = true, pce = true),
  PCE_OBSERVE("POST", "/pce/observe", fullOnly = true, gated = true, pce = true, screen = true),
  PCE_ACT("POST", "/pce/act", fullOnly = true, gated = true, pce = true, screen = true),
  PCE_PACK("POST", "/pce/pack", fullOnly = true, gated = true, pce = true, screen = true),
  HANDS_OVER("POST", "/hands/over", fullOnly = true, gated = true, screen = true),
  HANDS_WAIT("POST", "/hands/wait", fullOnly = true, gated = true),
}
