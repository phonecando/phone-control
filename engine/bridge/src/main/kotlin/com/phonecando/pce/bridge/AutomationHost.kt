package com.phonecando.pce.bridge

import com.phonecando.pce.gates.appaccess.AppGate
import org.json.JSONObject

// The screen driver the router hands its PCE calls to: the accessibility
// service's PCE adapter in a build that has one, nothing in one without.
// Who may drive is the phone-control lock's call
// (gates/controllock/ControlLock), not the host's. A screen call's gate
// judges every look and action inside it (gates/appaccess/AppGate); null:
// no app checks.
interface AutomationHost {
  val connected: Boolean
  fun describe(): JSONObject
  fun observe(options: JSONObject, gate: AppGate? = null): JSONObject
  fun act(request: JSONObject, gate: AppGate? = null): JSONObject
  fun pack(request: JSONObject, gate: AppGate? = null): JSONObject

  /** The package of the app in front, for the app-access gate; null when unknown. */
  fun frontPackage(): String? = null
}

// A host call found the accessibility service gone after the bridge's
// connected check; the bridge answers 503 automation_unavailable for it.
class AutomationUnavailableException(message: String) : IllegalStateException(message)
