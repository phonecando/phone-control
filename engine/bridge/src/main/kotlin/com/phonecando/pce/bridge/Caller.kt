package com.phonecando.pce.bridge

/**
 * An agent as the bridge knows it: [id] and [name] for the gates and the
 * screen line, and the bearer token its engine got at launch
 * ([bridgeToken], empty for none), which is how a request names it
 * (CallerAuth). The host's own agent record implements it.
 */
interface Caller {
  val id: String
  val name: String
  val bridgeToken: String
}
