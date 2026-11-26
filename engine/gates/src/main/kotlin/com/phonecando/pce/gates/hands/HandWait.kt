package com.phonecando.pce.gates.hands

/** How a wait for the hand-over ended. */
enum class HandWait {
  /** You handed the phone back (or it was never handed over). */
  BACK,

  /** Still yours, in WATCH: the time ran out. */
  WATCH,

  /** You stopped the agent: it ends its turn. */
  PAUSED,
}
