package com.phonecando.pce.service

import com.phonecando.pce.gates.appaccess.AppGate
import org.json.JSONObject

/**
 * The accessibility service's PCE adapter as the bridge's host meets it:
 * the PCE routes' JSON in, the PCE's answers out, each screen call under
 * its app gate. One per connected service; [close] ends its PCE.
 */
interface PceAutomation {
  fun describe(): JSONObject
  fun observe(options: JSONObject, gate: AppGate?): JSONObject
  fun act(request: JSONObject, gate: AppGate?): JSONObject
  fun pack(request: JSONObject, gate: AppGate?): JSONObject
  fun frontPackage(): String?
  fun close()
}
