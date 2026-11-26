package com.phonecando.pce.bridge

import com.phonecando.pce.gates.hands.HandBy
import com.phonecando.pce.gates.hands.HandMode
import com.phonecando.pce.gates.hands.HandOver
import com.phonecando.pce.gates.hands.HandWait
import com.phonecando.pce.gates.hands.HandsText
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// The phone routes behind the host's ports, called straight (the router's
// gates are BridgeRouterTest's): what each checks before the port runs,
// and the replies.
class PhoneActionRoutesTest {
  private val a1 = agent("a1", "opencode")

  private fun call(route: PhoneRoute, body: JSONObject = JSONObject()) = BridgeCall(a1, route, body)

  private fun json(reply: Reply) = JSONObject(reply.body)

  // --- /open, /share, /app

  private val started = mutableListOf<String>()
  private var noActivity = false
  private val activities = object : Activities {
    override fun scheme(url: String): String? = url.substringBefore(':', "").ifEmpty { null }

    override fun view(url: String) {
      if (noActivity) throw IllegalStateException("nothing handles it")
      started += "view $url"
    }

    override fun share(text: String, title: String?) {
      started += "share $text ${title ?: "-"}"
    }
  }
  private var back: AppReturn.Back? = AppReturn.Back(started = true, notified = false)
  private val returns = mutableListOf<String>()
  private val appReturn = AppReturn<TestCaller> { caller, tab, notify ->
    returns += "${caller.id} $tab $notify"
    back
  }
  private val activityRoutes = ActivityRoutes(activities, appReturn).table()

  private fun activity(route: PhoneRoute, body: JSONObject = JSONObject()) = activityRoutes.getValue(route)(call(route, body))

  @Test fun openStartsOnlyTheAllowedSchemes() {
    assertEquals(200, activity(PhoneRoute.OPEN, JSONObject().put("url", "https://example.com")).status)
    assertEquals(listOf("view https://example.com"), started)
    assertEquals("bad_scheme", json(activity(PhoneRoute.OPEN, JSONObject().put("url", "intent:#Intent;end"))).getString("code"))
    assertEquals("bad_scheme", json(activity(PhoneRoute.OPEN, JSONObject().put("url", "file:///sdcard/x"))).getString("code"))
    assertEquals("bad_url", json(activity(PhoneRoute.OPEN, JSONObject().put("url", "example.com"))).getString("code"))
    assertEquals("bad_request", json(activity(PhoneRoute.OPEN)).getString("code"))
    assertEquals(1, started.size)
  }

  @Test fun nothingToStartIsAnAppLevelFailure() {
    noActivity = true
    val reply = activity(PhoneRoute.OPEN, JSONObject().put("url", "tel:123"))
    assertEquals(200, reply.status)
    assertEquals("no_activity", json(reply).getString("code"))
  }

  @Test fun shareNeedsText() {
    assertEquals(400, activity(PhoneRoute.SHARE).status)
    assertEquals(200, activity(PhoneRoute.SHARE, JSONObject().put("text", "hi").put("title", "T")).status)
    assertEquals(listOf("share hi T"), started)
  }

  @Test fun appBringsTheAppBackOnATab() {
    val data = json(activity(PhoneRoute.APP, JSONObject().put("notify", true))).getJSONObject("data")
    assertTrue(data.getBoolean("started"))
    assertFalse(data.getBoolean("notified"))
    assertEquals(listOf("a1 home true"), returns)
    back = null
    assertEquals("no_activity", json(activity(PhoneRoute.APP)).getString("code"))
  }

  // --- /clipboard

  private var clip: String? = null
  private val clipboardRoutes = ClipboardRoutes<TestCaller>(object : Clipboard {
    override fun set(text: String) {
      clip = text
    }

    override fun get(): String? = clip
  }).table()

  private fun clipboard(body: JSONObject) = clipboardRoutes.getValue(PhoneRoute.CLIPBOARD)(call(PhoneRoute.CLIPBOARD, body))

  @Test fun clipboardSetsAndReads() {
    val empty = json(clipboard(JSONObject().put("action", "get"))).getJSONObject("data")
    assertTrue(empty.isNull("text"))
    assertFalse(empty.getBoolean("available"))
    clipboard(JSONObject().put("action", "set").put("text", "hello"))
    assertEquals("hello", json(clipboard(JSONObject().put("action", "get"))).getJSONObject("data").getString("text"))
    assertEquals(400, clipboard(JSONObject().put("action", "wipe")).status)
  }

  // --- /hands/*

  private var handOver: HandOver? = null
  private var wait = HandWait.BACK
  private val desk = object : HandsDesk {
    override fun agent(agentId: String, agentName: String, why: String, private: Boolean): HandOver? {
      if (handOver != null) return null
      val mode = if (private) HandMode.PAUSED else HandMode.WATCH
      return HandOver(agentId, agentName, mode, HandBy.AGENT, why, 0).also { handOver = it }
    }

    override fun await(agentId: String, timeoutMs: Long) = wait
  }
  private val handsRoutes = HandsRoutes<TestCaller>(desk).table()

  private fun hands(route: PhoneRoute, body: JSONObject = JSONObject()) = handsRoutes.getValue(route)(call(route, body))

  @Test fun handOverNeedsOneShortLine() {
    assertEquals(400, hands(PhoneRoute.HANDS_OVER).status)
    assertEquals(400, hands(PhoneRoute.HANDS_OVER, JSONObject().put("why", "x".repeat(121))).status)
    assertEquals(400, hands(PhoneRoute.HANDS_OVER, JSONObject().put("why", "pay").put("private", "yes")).status)
    assertEquals(null, handOver)
    hands(PhoneRoute.HANDS_OVER, JSONObject().put("why", "pay\nnow"))
    assertEquals("pay now", handOver!!.why)
  }

  @Test fun aPrivateHandOverAnswersAtOnce() {
    val data = json(hands(PhoneRoute.HANDS_OVER, JSONObject().put("why", "password").put("private", true))).getJSONObject("data")
    assertEquals("private", data.getString("mode"))
    assertEquals(HandsText.HANDED_PRIVATE, data.getString("message"))
    assertEquals("user_has_controls", json(hands(PhoneRoute.HANDS_OVER, JSONObject().put("why", "again"))).getString("code"))
  }

  @Test fun waitsSayHowTheyEnded() {
    assertTrue(json(hands(PhoneRoute.HANDS_WAIT)).getJSONObject("data").getBoolean("back"))
    wait = HandWait.WATCH
    assertEquals(HandsText.STILL_YOURS, json(hands(PhoneRoute.HANDS_WAIT)).getJSONObject("data").getString("message"))
    wait = HandWait.PAUSED
    assertEquals(HandsText.PAUSED, json(hands(PhoneRoute.HANDS_WAIT)).getJSONObject("data").getString("message"))
    assertEquals(400, hands(PhoneRoute.HANDS_WAIT, JSONObject().put("timeoutSec", "soon")).status)
  }
}
