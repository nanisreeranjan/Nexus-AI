package com.nexus.ai

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.Bundle

class NexusAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile var pendingText: String? = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val text = pendingText ?: return
        val root = rootInActiveWindow ?: return
        val field = findEditable(root) ?: return
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        val success = field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        if (success) pendingText = null
    }

    private fun findEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable && node.isEnabled) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findEditable(child)
            if (found != null) return found
        }
        return null
    }

    override fun onInterrupt() = Unit
}
