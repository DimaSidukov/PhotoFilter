package com.example.photofilter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.net.Uri
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import jp.wasabeef.glide.transformations.BitmapTransformation
import jp.wasabeef.glide.transformations.BlurTransformation
import jp.wasabeef.glide.transformations.gpu.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class PhotoFilter(
    val name: String,
    val bitmap: Bitmap?
)

class PhotoFilterHandler(
    private val context: Context,
    private val origUri: Uri,
    private val size: Pair<Int, Int>,
    private val onBitmapUpdated: (PhotoFilter) -> Unit
) {

    private val scope = CoroutineScope(Dispatchers.Main)

    private val filters = listOf(
        Pair(SepiaFilterTransformation(), "Sepia"),
        Pair(ToonFilterTransformation(), "Toon"),
        Pair(ContrastFilterTransformation(), "Contrast"),
        Pair(InvertFilterTransformation(), "Invert"),
        Pair(PixelationFilterTransformation(), "Pixelation"),
        Pair(SketchFilterTransformation(), "Sketch"),
        Pair(BrightnessFilterTransformation(), "Brightness"),
        Pair(KuwaharaFilterTransformation(), "Kuwahara"),
        Pair(VignetteFilterTransformation(), "Vignette"),
        Pair(BlurTransformation(), "Blur")
    )

    companion object {
        private const val LOWER_INDEX = 0
    }

    private var idx = 0

    fun updateCurrentFilter(a: Float, b: Float) {
        when (idx) {
            1 -> setFilter(SepiaFilterTransformation(a), "")
            2 -> setFilter(ToonFilterTransformation(a, b * 10), "")
            3 -> setFilter(ContrastFilterTransformation(a * 4), "")
            5 -> setFilter(PixelationFilterTransformation(a * 10), "")
            7 -> setFilter(BrightnessFilterTransformation((a * 2) - 1), "")
            8 -> setFilter(KuwaharaFilterTransformation((a * 25).toInt()), "")
            9 -> setFilter(
                VignetteFilterTransformation(
                    PointF(0.5f, 0.5f),
                    floatArrayOf(0.0f, 0.0f, 0.0f),
                    a,
                    b
                ),
                ""
            )
            10 -> setFilter(BlurTransformation((a * 25).toInt(), (b * 5).toInt()), "")
        }
    }

    private fun getDefaultParameters(setDefaultValues: (Float, Float) -> Unit) {
        when(idx) {
            1 -> setDefaultValues(1.0f, 0.0f)
            2 -> setDefaultValues(0.2f, 1.0f)
            3 -> setDefaultValues(1.0f, 0.0f)
            5 -> setDefaultValues(1.0f, 0.0f)
            7 -> setDefaultValues(0.5f, 0.0f)
            8 -> setDefaultValues(1.0f, 0.0f)
            9 -> setDefaultValues(0.0f, 0.75f)
            10 -> setDefaultValues(1.0f, 1.0f)
        }
    }

    fun nextFilter(setDefaultValues: (Float, Float) -> Unit) {
        if (idx == filters.size) idx = 0 else idx++
        getFilterByIdx(setDefaultValues)
    }

    fun previousFilter(setDefaultValues: (Float, Float) -> Unit) {
        if (idx == LOWER_INDEX) idx = filters.size else idx--
        getFilterByIdx(setDefaultValues)
    }

    private fun getFilterByIdx(setDefaultValues: (Float, Float) -> Unit) {
        when (idx) {
            0 -> {
                Glide.with(context)
                    .asBitmap()
                    .load(origUri)
                    .apply(RequestOptions().override(size.first, size.second))
                    .into(labelTarget("Original"))
            }
            else -> setFilter(idx - 1)
        }
        getDefaultParameters(setDefaultValues)
    }

    private fun setFilter(filterIdx: Int) =
        setFilter(filters[filterIdx].first, filters[filterIdx].second)

    private fun setFilter(filter: BitmapTransformation, name: String) {
        Glide.with(context)
            .asBitmap()
            .load(origUri)
            .apply(RequestOptions().override(size.first, size.second))
            .transform(filter)
            .into(labelTarget(name))
    }

    private fun labelTarget(name: String) = object : CustomTarget<Bitmap>() {
        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
            val filter = PhotoFilter(name, resource)
            onBitmapUpdated(filter)
        }

        override fun onLoadCleared(placeholder: Drawable?) {
        }
    }
}