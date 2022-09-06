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
import android.widget.ImageView.ScaleType
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.utils.widget.ImageFilterView
import androidx.core.app.ActivityCompat
import com.google.android.material.button.MaterialButton


class MainActivity : AppCompatActivity() {

    private lateinit var button: MaterialButton
    private lateinit var imageView: ImageFilterView
    private lateinit var textView: TextView

    private lateinit var imgBitmap: Bitmap
    private lateinit var photoFilterHandler: PhotoFilterHandler

    private val activityResultLauncher = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { imgUri ->
                imgBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(contentResolver, imgUri)
                    )
                else
                    MediaStore.Images.Media.getBitmap(contentResolver, imgUri)
                photoFilterHandler = PhotoFilterHandler(this, imgUri, ::updateView)
                imageView.setImageBitmap(imgBitmap)
                imageView.scaleType =
                    if (imgBitmap.height > imgBitmap.width) ScaleType.FIT_CENTER else ScaleType.FIT_START
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

        imageView.onSwipe(
            toLeft = {
                photoFilterHandler.nextFilter()
            },
            toRight = {
                photoFilterHandler.previousFilter()
            }
        )
    }

    private fun updateView(photoFilter: PhotoFilter) {
        imageView.setImageBitmap(null)
        imageView.setImageBitmap(photoFilter.bitmap)
        textView.text = photoFilter.name
        textView.setTextColor(if (photoFilter.bitmap!!.getPixel(0, 0) < -100) Color.WHITE else Color.BLACK)
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