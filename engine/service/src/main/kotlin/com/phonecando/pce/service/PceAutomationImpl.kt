package com.phonecando.pce.service

import android.os.Build
import com.phonecando.pce.android.A11yBackend
import com.phonecando.pce.android.A11yServiceProvider
import com.phonecando.pce.api.ActOptions
import com.phonecando.pce.api.Action
import com.phonecando.pce.api.Bounds
import com.phonecando.pce.api.ExperimentalPceApi
import com.phonecando.pce.api.Node
import com.phonecando.pce.api.NodeFlag
import com.phonecando.pce.api.ObserveOptions
import com.phonecando.pce.api.PceError
import com.phonecando.pce.api.PceResult
import com.phonecando.pce.api.Snapshot
import com.phonecando.pce.api.SnapshotId
import com.phonecando.pce.api.Window
import com.phonecando.pce.core.Pce
import com.phonecando.pce.gates.appaccess.AppGate
import com.phonecando.pce.json.PceJson
import com.phonecando.pce.keystore.PhoneApps
import com.phonecando.pce.service.appgate.AppGateCall
import com.phonecando.pce.service.appgate.AppGatedBackend
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

// The accessibility service's PCE adapter. The bridge only sees
// PceAutomation. The backend judges each look and action by the bridge
// call's app gate, which rides in the call's coroutine (AppGatedBackend),
// and keeps the host's overlay out of the agent's way (GuardedBackend).
// The host adds its overlay, its packs and what it does with each
// screenshot an agent was shown (ServiceHooks).
@OptIn(ExperimentalPceApi::class)
class PceAutomationImpl(
  private val service: PceAccessibilityService,
  overlay: OverlayGuard = OverlayGuard.NONE,
  hooks: ServiceHooks = ServiceHooks.NONE,
) : PceAutomation {
  private val backend = AppGatedBackend(
    GuardedBackend(A11yBackend(A11yServiceProvider { service }), overlay, service.packageName),
    service::frontPackage,
    service::appWindowPackages,
    PhoneApps(service)::named,
    hooks::shotTaken,
  )
  private val pce = Pce.create(backend)
  private val packs = hooks.packs(service, pce)

  override fun describe(): JSONObject {
    val capabilities = JSONArray()
    backend.capabilities.forEach { capabilities.put(it.name.lowercase()) }
    return JSONObject()
      .put("contract", "1.0")
      .put("pce", "0.1.0")
      .put("backend", backend.id)
      .put("capabilities", capabilities)
      .put("sdk", Build.VERSION.SDK_INT)
      .put("model", Build.MODEL)
      .put("automation", true)
      // Only an unlocked agent gets here: /pce/describe is behind the lock.
      .put("unlocked", true)
      .put("packs", packs.describeJson())
  }

  override fun observe(options: JSONObject, gate: AppGate?): JSONObject = runBlocking(gated(gate)) {
    val request = ObserveOptions(
      includeInvisible = options.optBoolean("includeInvisible", false),
      includeWindows = options.optBoolean("windows", true),
      screenshot = options.optBoolean("screenshot", false),
      maxNodes = options.optInt("maxNodes", 500).coerceIn(1, 2_000),
      timeoutMs = options.optLong("timeoutMs", 5_000).coerceIn(1_000, 15_000),
    )
    when (val result = pce.observe(request)) {
      // What AppGatedBackend left out, and a screenshot asked for that it didn't take.
      is PceResult.Ok -> result.value.toJson().apply {
        gate?.hidden?.takeIf { it.isNotEmpty() }?.let { put("hidden", hiddenJson(it)) }
        if (request.screenshot && result.value.screenshot == null) put("screenshotWithheld", WITHHELD)
      }
      is PceResult.Err -> error(result.error)
    }
  }

  private fun hiddenJson(hidden: List<AppGate.Hidden>) = JSONArray().apply {
    hidden.forEach { put(JSONObject().putOpt("package", it.pkg).put("label", it.label).put("what", it.what)) }
  }

  override fun act(request: JSONObject, gate: AppGate?): JSONObject = runBlocking(gated(gate)) {
    val snapshotId = request.optLong("snapshotId", 0L)
    if (snapshotId <= 0L) return@runBlocking badRequest("snapshotId is required")
    val actionElement = request.opt("action")
      ?: return@runBlocking badRequest("action is required")
    val actionText = when (actionElement) {
      is JSONObject -> actionElement.toString()
      is String -> actionElement
      else -> return@runBlocking badRequest("action must be an object")
    }
    val action = when (val decoded = PceJson.decodeActionStrict(actionText)) {
      is PceResult.Ok -> decoded.value
      is PceResult.Err -> return@runBlocking error(decoded.error)
    }

    // Decode the postcondition before running the action: a bad selector must
    // not leave a side effect behind.
    val expect = request.opt("expect")?.let { element ->
      val text = when (element) {
        is JSONObject -> element.toString()
        is String -> element
        else -> return@runBlocking badRequest("expect must be an object")
      }
      when (val decoded = PceJson.decodeSelectorStrict(text)) {
        is PceResult.Ok -> decoded.value
        is PceResult.Err -> return@runBlocking error(decoded.error)
      }
    }

    val timeoutMs = request.optLong("timeoutMs", 30_000L).coerceIn(1_000L, 120_000L)
    when (val result = pce.act(SnapshotId(snapshotId), action, ActOptions(timeoutMs))) {
      is PceResult.Err -> error(result.error)
      is PceResult.Ok -> {
        val data = JSONObject()
          .put("snapshotId", result.value.snapshotId.value)
          .put("backend", result.value.backend)
          .put("ms", result.value.durationMs)
        if (expect != null) {
          val expectMs = request.optLong("expectTimeoutMs", 5_000L).coerceIn(500L, 30_000L)
          when (val waited = pce.act(result.value.snapshotId, Action.Wait(expect, expectMs), ActOptions(expectMs))) {
            is PceResult.Err -> error(waited.error)
            is PceResult.Ok -> data.put("expect", true)
          }
        } else {
          data
        }
      }
    }
  }

  override fun pack(request: JSONObject, gate: AppGate?): JSONObject = packs.handle(request, gated(gate))

  override fun frontPackage(): String? = service.frontPackage()

  override fun close() {
    runCatching { runBlocking { pce.close() } }
  }

  private fun gated(gate: AppGate?): CoroutineContext = gate?.let(::AppGateCall) ?: EmptyCoroutineContext

  private fun badRequest(message: String): JSONObject = PceReplies.badRequest(message)

  private fun error(error: PceError): JSONObject = PceReplies.error(error)

  private fun Snapshot.toJson(): JSONObject {
    val out = JSONObject()
      .put("snapshotId", id.value)
      .put("backend", backend)
      .put("eventSeq", service.eventSeq)
      .put("stable", stable)
      .put("truncated", truncated)
      .put("focused", focusedNodeId?.value ?: JSONObject.NULL)
      .put("device", JSONObject()
        .put("sdk", device.sdk)
        .put("model", device.model ?: "")
        .put("display", JSONArray().put(device.displayWidth).put(device.displayHeight))
        .put("rotation", device.rotation)
        .put("screenOn", device.screenOn)
        .put("keyguard", device.keyguard)
        .put("ime", device.imeVisible))
      .put("windows", JSONArray().apply { windows.forEach { put(it.toJson()) } })
      .put("nodes", JSONArray().apply { nodes.forEach { put(it.toJson()) } })
    putOpt(out, "package", packageName)
    // The published PCE AAR may predate its optional activity provider;
    // the host service already tracks the same window-state event.
    putOpt(out, "activity", activityName ?: service.activityName(packageName))
    screenshot?.let {
      out.put("screenshot", JSONObject()
        .put("path", it.path)
        .put("w", it.width)
        .put("h", it.height)
        .put("mimeType", it.mimeType)
        .put("secure", it.secure))
    }
    return out
  }

  private fun Window.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("type", type.name.lowercase())
    .put("layer", layer)
    .put("bounds", bounds.toJson())
    .put("active", active)
    .put("focused", focused)
    .put("package", packageName ?: "")

  private fun Node.toJson(): JSONObject = JSONObject()
    .put("i", id.value)
    .put("depth", depth)
    .put("bounds", bounds.toJson())
    .put("clickable", clickable)
    .put("editable", editable)
    .put("scrollable", scrollable)
    .put("visible", visible)
    .put("enabled", enabled)
    .put("focused", has(NodeFlag.FOCUSED))
    .put("selected", has(NodeFlag.SELECTED))
    .put("checked", has(NodeFlag.CHECKED))
    .put("password", has(NodeFlag.PASSWORD))
    .apply {
      parentId?.let { put("parent", it.value) }
      putOpt(this, "pkg", packageName)
      putOpt(this, "cls", className)
      putOpt(this, "id", viewId)
      putOpt(this, "text", text)
      putOpt(this, "desc", contentDescription)
    }

  private fun Bounds.toJson(): JSONArray = JSONArray()
    .put(left)
    .put(top)
    .put(right)
    .put(bottom)

  private fun putOpt(out: JSONObject, name: String, value: String?) {
    if (!value.isNullOrEmpty()) out.put(name, value)
  }

  private companion object {
    const val WITHHELD = "No screenshot: it would show apps you may not use (the home screen's widgets and Recents " +
      "cards, or the parts listed in hidden). Read the nodes instead."
  }
}
