package com.example.photofilter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import jp.wasabeef.glide.transformations.BlurTransformation
import jp.wasabeef.glide.transformations.gpu.*

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

    private val filters = listOf(
        Pair(SepiaFilterTransformation(), "Sepia"),
        Pair(ToonFilterTransformation(), "Toon"),
        Pair(ContrastFilterTransformation(), "Contrast"),
        Pair(InvertFilterTransformation(), "Invert"),
        Pair(PixelationFilterTransformation(), "Pixelation"),
        Pair(SketchFilterTransformation(), "Sketch"),
        Pair(SwirlFilterTransformation(), "Swirl"),
        Pair(BrightnessFilterTransformation(), "Brightness"),
        Pair(KuwaharaFilterTransformation(), "Kuwahara"),
        Pair(VignetteFilterTransformation(), "Vignette"),
        Pair(BlurTransformation(), "Blur")
    )

    companion object {
        private const val LOWER_INDEX = 0
    }

    private var idx = 0

    fun nextFilter() {
        if (idx == filters.size) idx = 0 else idx++
        getFilterByIdx()
    }

    fun previousFilter() {
        if (idx == LOWER_INDEX) idx = filters.size else idx--
        getFilterByIdx()
    }

    private fun getFilterByIdx() {
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
    }

    private fun setFilter(filterIdx: Int) {
        Glide.with(context)
            .asBitmap()
            .load(origUri)
            .apply(RequestOptions().override(size.first, size.second))
            .transform(filters[filterIdx].first)
            .into(labelTarget(filters[filterIdx].second))
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