package autojs.api

import android.app.Activity
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.content.res.Resources
import android.graphics.Point
import android.util.DisplayMetrics
import android.view.Surface.ROTATION_0
import android.view.WindowManager

/**
 * Created by Stardust on Apr 26, 2017.
 */
@Suppress("unused")
class ScreenMetrics(private var designWidth: Int, private var designHeight: Int) {

    constructor() : this(0, 0)

    fun setScreenMetrics(width: Int, height: Int) {
        designWidth = width
        designHeight = height
    }

    @JvmOverloads
    fun scaleX(x: Int, width: Int = designWidth) = when {
        width == 0 || !mIsInitialized -> x
        else -> x * deviceScreenWidth / width
    }

    @JvmOverloads
    fun scaleY(y: Int, height: Int = designHeight) = when {
        height == 0 || !mIsInitialized -> y
        else -> y * deviceScreenHeight / height
    }

    @JvmOverloads
    fun rescaleX(x: Int, width: Int = designWidth) = when {
        width == 0 || !mIsInitialized -> x
        else -> x * width / deviceScreenWidth
    }

    @JvmOverloads
    fun rescaleY(y: Int, height: Int = designHeight) = when {
        height == 0 || !mIsInitialized -> y
        else -> y * height / deviceScreenHeight
    }

    companion object {

        var resources: Resources? = null

        private var mIsInitialized = false

        private var mPrivateWindowManager: WindowManager? = null
        private var windowManager: WindowManager
            get() = requireNotNull(mPrivateWindowManager) {
                "Call ScreenMetrics#init first"
            }
            set(value) {
                mPrivateWindowManager = value
            }

        @Suppress("DEPRECATION")
        @JvmStatic
        val rotation: Int
            get() = windowManager.defaultDisplay?.rotation ?: ROTATION_0

        @JvmStatic
        val orientation: Int
            get() = resources?.configuration?.orientation ?: ORIENTATION_PORTRAIT

        @JvmStatic
        val isScreenPortrait: Boolean
            get() = orientation == ORIENTATION_PORTRAIT

        @JvmStatic
        val isScreenLandscape: Boolean
            get() = orientation == ORIENTATION_LANDSCAPE

        @JvmStatic
        @Suppress("DEPRECATION")
        val deviceScreenWidth: Int
            get() {
                // resources?.displayMetrics?.widthPixels?.takeIf { it > 0 }?.let { return it }

                val metricsLegacy = DisplayMetrics()
                return windowManager.defaultDisplay.apply { getRealMetrics(metricsLegacy) }
                    .let { display ->
                        maxOf(metricsLegacy.widthPixels, display.width, 0)
                    }
            }

        @JvmStatic
        @Suppress("DEPRECATION")
        val deviceScreenHeight: Int
            get() {
                // resources?.displayMetrics?.heightPixels?.takeIf { it > 0 }?.let { return it }

                val metricsLegacy = DisplayMetrics()
                return windowManager.defaultDisplay.apply { getRealMetrics(metricsLegacy) }
                    .let { display ->
                        maxOf(metricsLegacy.heightPixels, display.height, 0)
                    }
            }

        @JvmStatic
        @Suppress("DEPRECATION")
        val deviceScreenDensity: Int
            get() {
                val metricsLegacy = DisplayMetrics()
                windowManager.defaultDisplay?.apply { getRealMetrics(metricsLegacy) }
                return metricsLegacy.densityDpi
            }

        @JvmStatic
        fun init(activity: Activity) {
            resources = activity.resources
            windowManager = activity.windowManager
            mIsInitialized = true
        }

        private fun toOriAwarePoint(a: Int, b: Int) = arrayOf(minOf(a, b), maxOf(a, b))
            .apply { if (isScreenLandscape) reverse() }
            .let { Point(it[0], it[1]) }

        @JvmStatic
        fun getOrientationAwareScreenWidth(orientation: Int) = when (orientation) {
            ORIENTATION_LANDSCAPE -> deviceScreenHeight
            else -> deviceScreenWidth
        }

        @JvmStatic
        fun getOrientationAwareScreenHeight(orientation: Int) = when (orientation) {
            ORIENTATION_LANDSCAPE -> deviceScreenWidth
            else -> deviceScreenHeight
        }

    }

}