package com.example.photofilter

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.utils.widget.ImageFilterView
import kotlin.math.abs

open class OnSwipeTouchListener(context: Context?) : View.OnTouchListener {

    private val gestureDetector: GestureDetector

    init {
        gestureDetector = GestureDetector(context, GestureListener())
    }

    open fun onSwipeLeft() {}
    open fun onSwipeRight() {}

    companion object {
        private const val SWIPE_DISTANCE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val distanceX = e2.x - e1.x
            val distanceY = e2.y - e1.y
            if (
                abs(distanceX) > abs(distanceY) &&
                abs(distanceX) > SWIPE_DISTANCE_THRESHOLD &&
                abs(velocityX) > SWIPE_VELOCITY_THRESHOLD
            ) {
                if (distanceX > 0) onSwipeRight() else onSwipeLeft()
                return true
            }
            return false
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
fun ImageFilterView.onSwipe(toLeft: () -> Unit, toRight: () -> Unit) =
    this.setOnTouchListener(object : OnSwipeTouchListener(this@onSwipe.context) {
        override fun onSwipeLeft() {
            toLeft()
        }

        override fun onSwipeRight() {
            toRight()
        }
    })

val Int.luminance: Int
    get() = 77 * (this shr 16 and 255) + 150 * (this shr 8 and 255) + 29 * (this and 255) shr 8
