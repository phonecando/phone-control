package com.phonecando.pce.bridge

import com.phonecando.pce.gates.appaccess.AppVerdict

/** What /unlock got: whether the user was asked, whether the lock is open now, and whether a notice went out. */
data class UnlockOutcome(val requested: Boolean, val unlocked: Boolean, val notified: Boolean)

/**
 * What /accessibility got: the user opened a settings step ([opened]; one
 * of the restricted-settings guide's, [guided]), the service is on
 * ([enabled]), they said "Not now" ([cancelled]), a notice went out.
 */
data class AccessibilityOutcome(
  val opened: Boolean,
  val enabled: Boolean,
  val cancelled: Boolean,
  val notified: Boolean,
  val guided: Boolean,
)

/**
 * What /app-access got: [asked] whether the user was asked at all (an app
 * already allowed or blocked answers at once), [allowed] and [blocked] as
 * the list stands after, the user's [answer] ("always", "once", "no"; null
 * for none), and whether the question reached them as a notice
 * ([notified]).
 */
data class AppOutcome(
  val asked: Boolean,
  val allowed: Boolean,
  val blocked: Boolean,
  val answer: String?,
  val notified: Boolean,
)

/**
 * The host's questions to the user, each shown in its own UI and answered
 * only by the user; the agent waits for the answer on its request's
 * thread. Each returns early once its condition holds whoever made it
 * hold (another agent's question, Settings).
 */
interface UserQuestions<C : Caller> {
  /** Unlock phone control for [agent]; waits up to `waitMs` for an answer or for [gateOn]. */
  fun unlock(agent: C, reason: String, waitMs: Long, gateOn: () -> Boolean): UnlockOutcome

  /** Turn the accessibility service on; waits up to `waitMs` for [connected] or a "Not now". */
  fun accessibility(agent: C, reason: String, waitMs: Long, connected: () -> Boolean): AccessibilityOutcome

  /** May agents use [pkg]; waits up to `waitMs` for an answer or for [verdict] to stop being ASK. */
  fun app(agent: C, pkg: String, label: String, waitMs: Long, verdict: () -> AppVerdict): AppOutcome
}
