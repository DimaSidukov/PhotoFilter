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
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.animation.AlphaAnimation
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.utils.widget.ImageFilterView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.yandex.mobile.ads.banner.AdSize
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    companion object {
        const val SIZE_CONSTRAINT = 1000
        const val INTERSTITIAL_DELAY = 30_000L
    }

    private lateinit var button: MaterialButton
    private lateinit var applyEffectsButton: MaterialButton
    private lateinit var saveImageButton: MaterialButton
    private lateinit var imageView: ImageFilterView
    private lateinit var textView: TextView
    private lateinit var firstParamSlider: Slider
    private lateinit var secondParamSlider: Slider
    private lateinit var banner: BannerAdView
    private lateinit var photoFilterHandler: PhotoFilterHandler
    private var interstitialAd: InterstitialAd? = null

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var isImageLoaded = MutableStateFlow(false)
    private var shouldShowInterstitial = MutableStateFlow(true)

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
        applyEffectsButton = findViewById(R.id.button_apply_effects)
        saveImageButton = findViewById(R.id.save_image_button)
        imageView = findViewById(R.id.filter_image)
        textView = findViewById(R.id.filter_name)
        firstParamSlider = findViewById(R.id.saturation_slider)
        secondParamSlider = findViewById(R.id.warmth_slider)

        loadBanner()
        loadInterstitial()

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

        applyEffectsButton.setOnClickListener {
            photoFilterHandler.updateCurrentFilter(firstParamSlider.value, secondParamSlider.value)
        }

        saveImageButton.setOnClickListener {
            val img = imageView.drawable.toBitmap()

            val storage = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val file = File(storage, "image.jpg")

            try {
                val fos = FileOutputStream(file)
                img.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                fos.close()

                Toast.makeText(this, "Image saved!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
        }

        scope.launch {
            isImageLoaded.runOnUiIfTrue {
                imageView.onSwipe(
                    toLeft = {
                        photoFilterHandler.nextFilter { a, b ->
                            firstParamSlider.value = a
                            secondParamSlider.value = b
                        }
                    },
                    toRight = {
                        photoFilterHandler.previousFilter { a, b ->
                            firstParamSlider.value = a
                            secondParamSlider.value = b
                        }
                    }
                )
                firstParamSlider.isVisible = true
                secondParamSlider.isVisible = true
                applyEffectsButton.isVisible = true
                saveImageButton.isVisible = true
                firstParamSlider.value = imageView.saturation
                secondParamSlider.value = imageView.warmth
            }
        }
        scope.launch {
            shouldShowInterstitial.collectLatest {
                if (it) {
                    delay(INTERSTITIAL_DELAY)
                    // interstitialAd?.loadAd(AdRequest.Builder().build())
                }
            }
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

    private fun loadBanner() {
        banner = findViewById(R.id.banner_view)
        banner.setAdUnitId("R-M-338228-20")
        banner.setAdSize(AdSize.flexibleSize(320, 50))
        banner.setBannerAdEventListener(object : BannerAdEventListener {
            override fun onAdLoaded() {
            }

            override fun onAdFailedToLoad(p0: AdRequestError) {
                Toast.makeText(this@MainActivity, p0.description, Toast.LENGTH_SHORT).show()
                Log.e("ERROR", p0.description)
            }

            override fun onAdClicked() {

            }

            override fun onLeftApplication() {

            }

            override fun onReturnedToApplication() {
            }

            override fun onImpression(p0: ImpressionData?) {

            }
        })
        banner.loadAd(AdRequest.Builder().build())
    }

    private fun loadInterstitial() {
        interstitialAd = InterstitialAd(this)
        interstitialAd?.setAdUnitId("R-M-DEMO-interstitial")
        interstitialAd?.setInterstitialAdEventListener(object : InterstitialAdEventListener {
            override fun onAdLoaded() {
                scope.launch { shouldShowInterstitial.emit(false) }
                interstitialAd?.show()
            }

            override fun onAdFailedToLoad(p0: AdRequestError) {

            }

            override fun onAdShown() {

            }

            override fun onAdDismissed() {
                scope.launch { shouldShowInterstitial.emit(true) }
            }

            override fun onAdClicked() {

            }

            override fun onLeftApplication() {

            }

            override fun onReturnedToApplication() {

            }

            override fun onImpression(p0: ImpressionData?) {

            }
        })
    }
}