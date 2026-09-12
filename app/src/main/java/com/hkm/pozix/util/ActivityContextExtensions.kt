package com.hkm.pozix.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Safely unwraps an Activity from Compose/dialog Context wrappers. */
fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}
