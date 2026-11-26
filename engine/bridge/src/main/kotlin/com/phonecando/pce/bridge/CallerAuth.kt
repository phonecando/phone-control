package com.phonecando.pce.bridge

import java.security.MessageDigest

// Who is calling: every engine gets its own bridge token at launch, so the
// bearer names the agent. Each candidate is compared in constant time and
// the loop never stops early, so the reply time says nothing about which
// token (or how much of one) matched. Pure: the host lists the callers.
object CallerAuth {
  fun bearer(header: String?): String? =
    header?.takeIf { it.startsWith(PREFIX) }?.substring(PREFIX.length)?.takeIf { it.isNotEmpty() }

  /** The calling agent among [callers], or null for a missing or unknown token (401). */
  fun <C : Caller> caller(header: String?, callers: Iterable<C>): C? {
    val provided = bearer(header)?.toByteArray() ?: return null
    var match: C? = null
    for (caller in callers) {
      if (caller.bridgeToken.isEmpty()) continue
      val same = MessageDigest.isEqual(caller.bridgeToken.toByteArray(), provided)
      if (same && match == null) match = caller
    }
    return match
  }

  private const val PREFIX = "Bearer "
}
