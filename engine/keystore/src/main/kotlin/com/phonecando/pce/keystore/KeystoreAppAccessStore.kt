package com.phonecando.pce.keystore

import android.content.Context
import com.phonecando.pce.gates.appaccess.AppAccessStore

// The user's answers about apps. A plain saved "Always" or "Allow all"
// would let an agent let itself into any app, so, as with the
// phone-control lock (KeystoreLockStore), each of those is signed with its
// own Keystore key that signs only right after the user's biometric or PIN
// (AuthBoundSigner), and the signature lives in prefs. Always is one key
// per app, so taking one app off (which needs no biometric) deletes its
// key and an older copy of its record never checks out again; Allow all
// is one key, deleted when it is turned off, and its record signs the No
// list with it. A record whose key is missing or was not made this way
// does not count. "No" is plain prefs: a forged one only blocks more, and
// one taken off makes Allow all's record fail (everything asks again).
class KeystoreAppAccessStore(context: Context) : AppAccessStore {
  private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
  private val signer = AuthBoundSigner(TAG)

  override fun loadAlways(): Set<String> =
    prefs.all.keys
      .filter { it.startsWith(ALWAYS_SIG) }
      .map { it.removePrefix(ALWAYS_SIG) }
      .filter { signer.verify(alwaysAlias(it), ALWAYS_MESSAGE + it, prefs.getString(ALWAYS_SIG + it, null)) }
      .toSet()

  override fun saveAlways(pkg: String): Boolean {
    val sig = signer.sign(alwaysAlias(pkg), ALWAYS_MESSAGE + pkg) ?: return false
    return prefs.edit().putString(ALWAYS_SIG + pkg, sig).commit()
  }

  override fun dropAlways(pkg: String) {
    prefs.edit().remove(ALWAYS_SIG + pkg).commit()
    signer.deleteKey(alwaysAlias(pkg))
  }

  override fun loadAllowAll(blocked: Set<String>): Boolean =
    signer.hasKey(ALL_ALIAS) && signer.verify(ALL_ALIAS, allMessage(blocked), prefs.getString(KEY_ALL_SIG, null))

  override fun saveAllowAll(blocked: Set<String>): Boolean {
    val sig = signer.sign(ALL_ALIAS, allMessage(blocked)) ?: return false
    return prefs.edit().putString(KEY_ALL_SIG, sig).commit()
  }

  override fun dropAllowAll() {
    prefs.edit().remove(KEY_ALL_SIG).commit()
    signer.deleteKey(ALL_ALIAS)
  }

  override fun loadBlocked(): Set<String> = prefs.getStringSet(KEY_BLOCKED, null)?.toSet() ?: emptySet()

  override fun saveBlocked(packages: Set<String>) {
    prefs.edit().putStringSet(KEY_BLOCKED, HashSet(packages)).commit()
  }

  private fun alwaysAlias(pkg: String) = ALWAYS_ALIAS + pkg

  // A package name has no comma, so the sorted list reads one way only.
  private fun allMessage(blocked: Set<String>) = ALL_MESSAGE + blocked.sorted().joinToString(",")

  private companion object {
    const val TAG = "PhoneCanDoAppAccess"
    const val PREFS = "phonecando_app_access"
    const val ALWAYS_SIG = "always_sig:"
    const val KEY_ALL_SIG = "allow_all_sig"
    const val KEY_BLOCKED = "blocked"
    const val ALWAYS_ALIAS = "phonecando_app_access_always_"
    const val ALL_ALIAS = "phonecando_app_access_allow_all"
    const val ALWAYS_MESSAGE = "phonecando app-access always "
    const val ALL_MESSAGE = "phonecando app-access allowAll v1 "
  }
}
