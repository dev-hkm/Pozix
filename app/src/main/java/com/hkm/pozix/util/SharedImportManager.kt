package com.hkm.pozix.util

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Singleton state holder for JSON content shared or opened from external apps.
 */
object SharedImportManager {
    val pendingJson = MutableStateFlow<String?>(null)
}
