package com.phonecando.pce.service.appgate

import com.phonecando.pce.gates.appaccess.AppGate
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/** The bridge call's app check, carried down the PCE's suspend calls to AppGatedBackend. */
class AppGateCall(val gate: AppGate) : AbstractCoroutineContextElement(AppGateCall) {
  companion object Key : CoroutineContext.Key<AppGateCall>
}
