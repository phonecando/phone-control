# keystore

`pce-keystore`, Android library. The gates' Android side.

- `KeystoreLockStore`, `KeystoreAppAccessStore` — the lock mode, a saved
  unlock, Always and Allow all, each signed with its own Android Keystore
  key that signs only within 15 seconds of the user's fingerprint or
  screen-lock PIN (`AuthBoundSigner`). A record that does not check out
  never grants anything. No is plain prefs: a forged one only blocks more.
- `PhoneApps` — an app's name, the default home screen, the app a link
  opens in, the apps with a given name.

The host shows the biometric prompt; these only sign right after it.
