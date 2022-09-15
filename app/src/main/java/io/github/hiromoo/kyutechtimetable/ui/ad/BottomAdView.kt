package io.github.hiromoo.kyutechtimetable.ui.ad

import android.app.Activity
import android.util.DisplayMetrics
import android.view.ViewGroup
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

class BottomAdView(private val activity: Activity, private val bottomAdViewContainer: ViewGroup) {
    private var adView: AdView = AdView(activity)

    private val adSize: AdSize
        get() {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
                val outMetrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                activity.windowManager?.defaultDisplay?.getMetrics(outMetrics)

                val density = outMetrics.density

                var adWidthPixels = bottomAdViewContainer.width.toFloat()
                if (adWidthPixels == 0f) {
                    adWidthPixels = outMetrics.widthPixels.toFloat()
                }

                val adWidth = (adWidthPixels / density).toInt()
                return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    activity,
                    adWidth
                )
            } else {
                val outMetrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                activity.windowManager?.defaultDisplay?.getMetrics(outMetrics)

                val density = activity.resources.displayMetrics.density

                var adWidthPixels = bottomAdViewContainer.width.toFloat()
                if (adWidthPixels == 0f) {
                    adWidthPixels =
                        activity.windowManager?.currentWindowMetrics?.bounds?.width()?.toFloat()
                            ?: 0f
                }

                val adWidth = (adWidthPixels / density).toInt()
                return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    activity,
                    adWidth
                )
            }
        }

    init {
        bottomAdViewContainer.addView(adView)
    }

    fun loadBanner() {
        adView.adUnitId = AD_UNIT_ID
        adView.setAdSize(adSize)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    companion object {
        private const val AD_UNIT_ID = "ca-app-pub-3872584665626955/6970400471"
    }
}