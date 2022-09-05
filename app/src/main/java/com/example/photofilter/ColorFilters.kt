package com.example.photofilter

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.constraintlayout.utils.widget.ImageFilterView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class PhotoFilter(
    val name: String = "",
    val filter: ImageFilterView.() -> Unit = { },
)

// i need to subscribe for something to update image with effect and text,
// but what exactly?
class PhotoFilterHandler {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    companion object {
        private const val LOWER_INDEX = 0
        private const val UPPER_INDEX = 10
    }

    private var idx = MutableStateFlow(LOWER_INDEX)

    fun nextFilter(): PhotoFilter {
        idx.value = if (idx.value == UPPER_INDEX) 0 else idx.value++
        return getFilterByIdx()
    }

    fun previousFilter(): PhotoFilter {
        idx.value = if (idx.value == LOWER_INDEX) 0 else idx.value--
        return getFilterByIdx()
    }

    private fun getFilterByIdx(): PhotoFilter {
        return when (idx.value) {
            0 -> PhotoFilter()
            1 -> PhotoFilter("Sepia") { toSepia() }
            else -> PhotoFilter()
        }
    }

}

fun ImageFilterView.toSepia() {
    val matrixA = ColorMatrix()
    // making image B&W
    // making image B&W
    matrixA.setSaturation(0f)

    val matrixB = ColorMatrix()
    // applying scales for RGB color values
    // applying scales for RGB color values
    matrixB.setScale(1f, .95f, .82f, 1.0f)
    matrixA.setConcat(matrixB, matrixA)

    val filter = ColorMatrixColorFilter(matrixA)
    this.colorFilter = filter
}