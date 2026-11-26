package com.phonecando.pce.service

import kotlin.coroutines.CoroutineContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * The host's packs (signed flows run by the PCE's pack runtime): where
 * they come from and are kept is the host's. A pack run's steps go
 * through the same gated backend as every other call: the host runs them
 * under [handle]'s `gated` context, which carries the call's app gate.
 */
interface Packs {
  /** The installed packs, for /pce/describe. */
  fun describeJson(): JSONArray

  /** One /pce/pack request (list, refresh, rollback, run), in the PCE adapter's {ok,...} shape. */
  fun handle(request: JSONObject, gated: CoroutineContext): JSONObject

  companion object {
    /** No packs: none listed, every request refused. */
    val NONE: Packs = object : Packs {
      override fun describeJson() = JSONArray()

      override fun handle(request: JSONObject, gated: CoroutineContext) = PceReplies.badRequest("no packs in this build")
    }
  }
}
