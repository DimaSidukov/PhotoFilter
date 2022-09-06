package com.example.photofilter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import jp.wasabeef.picasso.transformations.gpu.*

class PhotoFilter(
    val name: String,
    val bitmap: Bitmap?
)

class PhotoFilterHandler(
    context: Context,
    private val origUri: Uri,
    private val onBitmapUpdated: (PhotoFilter) -> Unit
) {

    private val filters = listOf(
        SepiaFilterTransformation(context),
        ToonFilterTransformation(context),
        ContrastFilterTransformation(context),
        InvertFilterTransformation(context),
        PixelationFilterTransformation(context),
        SketchFilterTransformation(context),
        SwirlFilterTransformation(context),
        BrightnessFilterTransformation(context),
        KuwaharaFilterTransformation(context),
        VignetteFilterTransformation(context)
    )

    companion object {
        private const val LOWER_INDEX = 0
        private const val UPPER_INDEX = 10
    }

    private var idx = 0

    fun nextFilter() {
        if (idx == UPPER_INDEX) idx = 0 else idx++
        getFilterByIdx()
    }

    fun previousFilter() {
        if (idx == LOWER_INDEX) idx = UPPER_INDEX else idx--
        getFilterByIdx()
    }

    private fun getFilterByIdx() {
        when (idx) {
            0 -> onDefaultImage()
            1 -> getFilter("Sepia")
            2 -> getFilter("Toon")
            3 -> getFilter("Contrast")
            4 -> getFilter("Invert")
            5 -> getFilter("Pixelation")
            6 -> getFilter("Sketch")
            7 -> getFilter("Swirl")
            8 -> getFilter("Brightness")
            9 -> getFilter("Kuwahara")
            10 -> getFilter("Vignette")
            else -> onDefaultImage()
        }
    }

    private fun onDefaultImage() {
        Picasso.get().load(origUri).into(labelTarget("Original"))
    }

    private fun getFilter(name: String) {
        filters[idx - 1].applyFilter(name)
    }

    private fun GPUFilterTransformation.applyFilter(name: String) =
        Picasso.get().load(origUri).transform(this@applyFilter).into(labelTarget(name))

    private fun labelTarget(name: String) = object : Target {
        override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
            val filter = PhotoFilter(name, bitmap)
            onBitmapUpdated(filter)
        }

        override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
        }

        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
        }
    }
}