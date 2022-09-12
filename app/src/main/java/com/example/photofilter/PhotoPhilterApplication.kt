package com.example.photofilter

import android.app.Application
import android.util.Log
import com.yandex.mobile.ads.common.MobileAds

class PhotoPhilterApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {
            Log.d("SDK", "initialized")
        }
    }
}