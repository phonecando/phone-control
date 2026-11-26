package com.phonecando.pce.keystore

import android.content.Context
import com.phonecando.pce.gates.controllock.LockMode
import com.phonecando.pce.gates.controllock.LockStore

// The phone-control lock's records: the mode and an install-mode unlock,
// each signed with its own Keystore key that signs only right after the
// user's biometric or PIN (AuthBoundSigner), the signature in prefs; a
// plain saved "unlocked" or mode would let an agent unlock itself.
// Dropping the unlock deletes its key. A record whose key is missing was
// never ours (a fresh install, or prefs restored from a backup without the
// Keystore): the mode reads as DEFAULT, the unlock as absent. A sign that
// fails after its new key replaced the old one leaves a key whose record
// does not check out: the mode reads as STRICTEST, the unlock as absent.
// Never deleting the key on a failed sign keeps a failure from reading as
// a fresh install's DEFAULT.
class KeystoreLockStore(context: Context) : LockStore {
  private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
  private val signer = AuthBoundSigner(TAG)

  override fun loadMode(): LockMode {
    if (!signer.hasKey(MODE_ALIAS)) return LockMode.DEFAULT
    val wire = prefs.getString(KEY_MODE, null)
    val mode = LockMode.fromWire(wire) ?: return LockMode.STRICTEST
    return if (signer.verify(MODE_ALIAS, MODE_MESSAGE + mode.wire, prefs.getString(KEY_MODE_SIG, null))) {
      mode
    } else {
      LockMode.STRICTEST
    }
  }

  override fun saveMode(mode: LockMode): Boolean {
    val sig = signer.sign(MODE_ALIAS, MODE_MESSAGE + mode.wire) ?: return false
    return prefs.edit().putString(KEY_MODE, mode.wire).putString(KEY_MODE_SIG, sig).commit()
  }

  override fun hasSavedUnlock(): Boolean =
    signer.hasKey(UNLOCK_ALIAS) && signer.verify(UNLOCK_ALIAS, UNLOCK_MESSAGE, prefs.getString(KEY_UNLOCK_SIG, null))

  override fun saveUnlock(): Boolean {
    val sig = signer.sign(UNLOCK_ALIAS, UNLOCK_MESSAGE) ?: return false
    return prefs.edit().putString(KEY_UNLOCK_SIG, sig).commit()
  }

  override fun dropUnlock() {
    prefs.edit().remove(KEY_UNLOCK_SIG).commit()
    signer.deleteKey(UNLOCK_ALIAS)
  }

  private companion object {
    const val TAG = "PhoneCanDoLock"
    const val PREFS = "phonecando_phone_control"
    const val KEY_MODE = "mode"
    const val KEY_MODE_SIG = "mode_sig"
    const val KEY_UNLOCK_SIG = "unlock_sig"
    const val MODE_ALIAS = "phonecando_phone_control_mode"
    const val UNLOCK_ALIAS = "phonecando_phone_control_unlock"
    const val MODE_MESSAGE = "phonecando phone-control mode "
    const val UNLOCK_MESSAGE = "phonecando phone-control unlocked"
  }
}
