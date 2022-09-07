package com.example.photofilter

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.animation.AlphaAnimation
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.utils.widget.ImageFilterView
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    companion object {
        const val SIZE_CONSTRAINT = 1000
    }

    private lateinit var button: MaterialButton
    private lateinit var imageView: ImageFilterView
    private lateinit var textView: TextView
    private lateinit var saturationSlider: Slider
    private lateinit var warmthSlider: Slider

    private lateinit var photoFilterHandler: PhotoFilterHandler

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var isImageLoaded = MutableStateFlow(false)

    private val activityResultLauncher = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { imgUri ->
                val imgBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(contentResolver, imgUri)
                    )
                else
                    MediaStore.Images.Media.getBitmap(contentResolver, imgUri)

                val ratio =
                    if (imgBitmap.width > SIZE_CONSTRAINT || imgBitmap.height > SIZE_CONSTRAINT) 1.2f else 1f
                var size = Pair(imgBitmap.width, imgBitmap.height)

                var width = size.first.toFloat()
                var height = size.second.toFloat()
                while (width > SIZE_CONSTRAINT.toFloat() && height > SIZE_CONSTRAINT.toFloat()) {
                    width /= ratio
                    height /= ratio
                }
                size = Pair(width.toInt(), height.toInt())
                val newBitmap = Bitmap.createScaledBitmap(imgBitmap, size.first, size.second, true)

                photoFilterHandler = PhotoFilterHandler(this, imgUri, size, ::updateView)
                scope.launch {
                    isImageLoaded.emit(true)
                }
                imageView.setImageBitmap(newBitmap)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button = findViewById(R.id.button)
        imageView = findViewById(R.id.filter_image)
        textView = findViewById(R.id.filter_name)
        saturationSlider = findViewById(R.id.saturation_slider)
        warmthSlider = findViewById(R.id.warmth_slider)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            1
        )

        button.setOnClickListener {
            activityResultLauncher.launch(Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            })
        }

        scope.launch {
            isImageLoaded.runOnUiIfTrue {
                imageView.onSwipe(
                    toLeft = {
                        photoFilterHandler.nextFilter()
                    },
                    toRight = {
                        photoFilterHandler.previousFilter()
                    }
                )
                saturationSlider.isVisible = true
                warmthSlider.isVisible = true
                saturationSlider.value = imageView.saturation
                warmthSlider.value = imageView.warmth
            }
        }

        saturationSlider.addOnChangeListener { slider, value, fromUser ->
            imageView.saturation = value
        }
        warmthSlider.addOnChangeListener { slider, value, fromUser ->
            imageView.warmth = value
        }
    }

    private suspend fun StateFlow<Boolean>.runOnUiIfTrue(action: () -> Unit) = this.collectLatest {
        if (it) runOnUiThread(action)
    }

    private fun updateView(photoFilter: PhotoFilter) {
        imageView.setImageBitmap(photoFilter.bitmap)
        textView.setTextColor(
            if (photoFilter.bitmap!!.getPixel(
                    0,
                    0
                ).luminance < 227
            ) Color.WHITE else Color.BLACK
        )
        textView.text = photoFilter.name
        textView.startAnimation(AlphaAnimation(0f, 0.8f).apply {
            duration = 500
            fillAfter = true
        })
        textView.startAnimation(AlphaAnimation(0.8f, 0f).apply {
            startOffset = 500
            duration = 500
            fillAfter = true
        })
    }
}