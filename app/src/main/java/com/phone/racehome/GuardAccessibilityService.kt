package com.phone.racehome

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * When armed: if a blocked package (telecom / carrier / paks / …) shows a window,
 * press Back and immediately re-open Race Home. Also tries to tap Force stop / Disable
 * when App Info screens are open.
 */
class GuardAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastPushAway = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Accessibility connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!Prefs.isArmed(this)) return

        val pkg = event.packageName?.toString() ?: return

        if (Blocklist.isBlocked(pkg)) {
            val now = SystemClock.uptimeMillis()
            if (now - lastPushAway < 400L) return
            lastPushAway = now
            Log.i(TAG, "Blocked UI from $pkg — pushing away")
            performGlobalAction(GLOBAL_ACTION_BACK)
            handler.postDelayed({
                performGlobalAction(GLOBAL_ACTION_HOME)
                val i = Intent(this, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                }
                startActivity(i)
            }, 120L)
            return
        }

        // Help finish Force stop / Disable taps on Settings App Info pages
        if (pkg == "com.android.settings" || pkg.contains("settings")) {
            val root = rootInActiveWindow ?: return
            tryClickByText(root, listOf("Force stop", "Force Stop", "FORCE STOP"))
            tryClickByText(root, listOf("Disable", "DISABLE", "Disable app"))
            tryClickByText(root, listOf("OK", "Force stop", "Delete", "Disable"))
        }
    }

    override fun onInterrupt() {
        // no-op
    }

    private fun tryClickByText(root: AccessibilityNodeInfo, labels: List<String>) {
        for (label in labels) {
            val nodes = root.findAccessibilityNodeInfosByText(label) ?: continue
            for (node in nodes) {
                if (node.isClickable && node.isEnabled) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return
                }
                var p = node.parent
                var depth = 0
                while (p != null && depth < 5) {
                    if (p.isClickable && p.isEnabled) {
                        p.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return
                    }
                    p = p.parent
                    depth++
                }
            }
        }
    }

    companion object {
        private const val TAG = "RaceHomeGuard"
    }
}
