package com.guguabc.kids

import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.guguabc.kids.design.GuguDimens
import kotlin.math.min

/** design.md §2 / §3.2 / §3.3：单 Activity、锁定横屏（Manifest）、隐藏系统栏。 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        if (BuildConfig.DEBUG) logDisplayMetrics()

        setContent { GuguApp() }
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    /** design.md §13 ① / 附录 C ①：仅 DEBUG 输出，用于确认 densityDpi 假设。 */
    private fun logDisplayMetrics() {
        val dm: DisplayMetrics = resources.displayMetrics
        val fixedDesignDensity = min(
            dm.widthPixels / GuguDimens.screenWidth.value,
            dm.heightPixels / GuguDimens.screenHeight.value,
        )
        Log.i(
            TAG,
            "sizePx=${dm.widthPixels}x${dm.heightPixels} density=${dm.density} " +
                "densityDpi=${dm.densityDpi} fixedDesignDensity=$fixedDesignDensity " +
                "fontScale=${resources.configuration.fontScale}",
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()   // 系统栏被手势临时唤出后再次隐藏
    }

    private companion object {
        const val TAG = "GuguMetrics"
    }
}
