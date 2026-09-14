package com.hkm.pozix.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LectureData(
    val title: String,
    val content: String
)

/**
 * Global reactive state holder for opening and reading theory lectures & study notes in Pozix.
 */
object LectureManager {
    private val _currentLecture = MutableStateFlow<LectureData?>(null)
    val currentLecture = _currentLecture.asStateFlow()

    fun openLecture(title: String, content: String) {
        _currentLecture.value = LectureData(title = title, content = content)
    }

    fun clear() {
        _currentLecture.value = null
    }
}
