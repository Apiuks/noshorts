package com.apiuks.noshorts

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ShortsService : AccessibilityService() {
    private var lastAction = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val prefs = getSharedPreferences("lock", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now >= prefs.getLong("end", 0L) || now - lastAction < 800) return
        val pkg = event?.packageName?.toString() ?: return
        val root = rootInActiveWindow ?: return

        if (pkg == "com.google.android.youtube") {
            if (isShorts(root, 0)) {
                lastAction = now
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        } else if (prefs.getBoolean("strict", false)) {
            if (root.findAccessibilityNodeInfosByText("Shorts Kilidi").isNotEmpty()) {
                lastAction = now
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }
    }

    private fun isShorts(node: AccessibilityNodeInfo?, depth: Int): Boolean {
        if (node == null || depth > 25) return false
        val id = node.viewIdResourceName ?: ""
        if (id.contains("reel_recycler") || id.contains("reel_player_page_container")) return true
        for (i in 0 until node.childCount) {
            if (isShorts(node.getChild(i), depth + 1)) return true
        }
        return false
    }

    override fun onInterrupt() {}
}
