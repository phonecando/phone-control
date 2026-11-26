package com.phonecando.pce.keystore

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import android.util.Base64
import android.util.Log
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature

// Signs a record with an Android Keystore key of its own that signs only
// within AUTH_WINDOW_S of the user's fingerprint (strong biometrics) or
// screen-lock PIN, pattern or password, and checks it again with the
// public key, which needs no biometric. Agents run as the app's Linux
// user, so they can write every file the app keeps; a record they could
// sign themselves would let one grant itself more. Every sign makes a new
// key under the record's alias, so an older copy of a record no longer
// checks out. A key that was not made this way does not count. Used by
// KeystoreLockStore and KeystoreAppAccessStore; [tag] names them in logs.
internal class AuthBoundSigner(private val tag: String) {
  // A new key under [alias] (replacing the old one), then its signature of
  // [message]; null when the user has not just authenticated or the
  // Keystore refuses. Without a fresh authentication the old key and its
  // record stay as they were; a sign that fails after the new key replaced
  // the old one leaves a record that does not check out.
  fun sign(alias: String, message: String): String? {
    if (!justAuthenticated(alias)) return null
    return try {
      val pair = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, PROVIDER)
        .apply { initialize(spec(alias)) }
        .generateKeyPair()
      val sig = Signature.getInstance(ALGORITHM).run {
        initSign(pair.private)
        update(message.toByteArray())
        sign()
      }
      Base64.encodeToString(sig, Base64.NO_WRAP)
    } catch (e: Exception) {
      Log.w(tag, "could not sign $alias", e)
      null
    }
  }

  fun verify(alias: String, message: String, sig: String?): Boolean {
    if (sig == null) return false
    return try {
      val entry = keys().getEntry(alias, null) as? KeyStore.PrivateKeyEntry ?: return false
      if (!authBound(entry.privateKey)) return false
      Signature.getInstance(ALGORITHM).run {
        initVerify(entry.certificate.publicKey)
        update(message.toByteArray())
        verify(Base64.decode(sig, Base64.NO_WRAP))
      }
    } catch (e: Exception) {
      Log.w(tag, "could not check $alias", e)
      false
    }
  }

  fun hasKey(alias: String): Boolean = try {
    keys().containsAlias(alias)
  } catch (e: Exception) {
    Log.w(tag, "keystore unavailable", e)
    false
  }

  fun deleteKey(alias: String) {
    try {
      val store = keys()
      if (store.containsAlias(alias)) store.deleteEntry(alias)
    } catch (e: Exception) {
      Log.w(tag, "could not delete $alias", e)
    }
  }

  // The old key under [alias] would sign now. Only "the user has not just
  // authenticated" says no: a missing key, or one Android voided when the
  // screen lock was removed, has nothing worth keeping.
  private fun justAuthenticated(alias: String): Boolean = try {
    val key = keys().getKey(alias, null) as? PrivateKey
    key?.let { Signature.getInstance(ALGORITHM).initSign(it) }
    true
  } catch (_: UserNotAuthenticatedException) {
    false
  } catch (_: Exception) {
    true
  }

  // A key we made: it signs only right after the user's biometric or PIN.
  // A key made any other way under our alias does not count.
  private fun authBound(key: PrivateKey): Boolean {
    val info = KeyFactory.getInstance(key.algorithm, PROVIDER).getKeySpec(key, KeyInfo::class.java)
    @Suppress("DEPRECATION")
    val window = info.userAuthenticationValidityDurationSeconds
    return info.isUserAuthenticationRequired && window in 1..AUTH_WINDOW_S
  }

  private fun spec(alias: String): KeyGenParameterSpec =
    KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
      .setDigests(KeyProperties.DIGEST_SHA256)
      .setUserAuthenticationRequired(true)
      .apply {
        if (Build.VERSION.SDK_INT >= 30) {
          setUserAuthenticationParameters(
            AUTH_WINDOW_S,
            KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
          )
        } else {
          @Suppress("DEPRECATION")
          setUserAuthenticationValidityDurationSeconds(AUTH_WINDOW_S)
        }
      }
      .build()

  private fun keys(): KeyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

  private companion object {
    const val PROVIDER = "AndroidKeyStore"
    const val ALGORITHM = "SHA256withECDSA"

    /** Seconds after the user's biometric or PIN in which a key signs. */
    const val AUTH_WINDOW_S = 15
  }
}
