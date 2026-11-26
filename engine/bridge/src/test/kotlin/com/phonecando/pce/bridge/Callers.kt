package com.phonecando.pce.bridge

// Test agents: an in-memory list that tests can shrink to model an engine
// leaving it, and two host routes that are not the phone's.
data class TestCaller(override val id: String, override val name: String, override val bridgeToken: String) : Caller

fun agent(id: String, name: String = id, token: String = "token-$id") = TestCaller(id, name, token)

class FakeDirectory(vararg agents: TestCaller) {
  val records = agents.toMutableList()

  fun all(): List<TestCaller> = records.toList()

  fun get(id: String): TestCaller? = records.firstOrNull { it.id == id }
}

enum class HostRoute(override val method: String, override val path: String) : BridgeRoute {
  PING("GET", "/ping"),
  NOTIFY("POST", "/notify"),
  ;

  override val fullOnly get() = false
  override val gated get() = false
  override val pce get() = false
  override val screen get() = false
}

/** Every route a test bridge serves. */
val ALL_ROUTES: List<BridgeRoute> = PhoneRoute.entries + HostRoute.entries
