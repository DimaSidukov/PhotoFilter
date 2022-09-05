package com.example.photofilter

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView.ScaleType
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.utils.widget.ImageFilterView
import androidx.core.app.ActivityCompat
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private lateinit var button: MaterialButton
    private lateinit var imageView: ImageFilterView

    private lateinit var imgBitmap: Bitmap
    private var photoFilterHandler = PhotoFilterHandler()

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val activityResultLauncher = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { imgUri ->
                imgBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(contentResolver, imgUri)
                    )
                else
                    MediaStore.Images.Media.getBitmap(contentResolver, imgUri)
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
                Toast.makeText(
                    this@MainActivity,
                    "right to left swipe",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}