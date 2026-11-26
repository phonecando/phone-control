package com.phonecando.pce.service

import com.phonecando.pce.api.PceError
import org.json.JSONArray
import org.json.JSONObject

/** The PCE adapter's failure shapes, {ok:false, code, message, recover?, candidates?}, which the bridge unwraps. */
object PceReplies {
  fun badRequest(message: String): JSONObject = JSONObject()
    .put("ok", false)
    .put("code", "bad_request")
    .put("message", message)

  fun error(error: PceError): JSONObject {
    val out = JSONObject()
      .put("ok", false)
      .put("code", error.code.name)
      .put("message", error.message)
    error.recovery?.let {
      out.put("recover", JSONObject().put("action", it.action).put("hint", it.hint ?: ""))
    }
    if (error.candidates.isNotEmpty()) {
      out.put("candidates", JSONArray(error.candidates.map { it.value }))
    }
    return out
  }
}
