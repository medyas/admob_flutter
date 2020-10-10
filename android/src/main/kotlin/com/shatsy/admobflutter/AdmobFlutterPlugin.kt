package com.shatsy.admobflutter

import android.content.Context
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import kotlin.collections.HashMap

fun createAdListener(channel: MethodChannel) : AdListener {
  return object: AdListener() {
    override fun onAdLoaded() = channel.invokeMethod("loaded", null)
    override fun onAdFailedToLoad(errorCode: Int) = channel.invokeMethod("failedToLoad", hashMapOf("errorCode" to errorCode))
    override fun onAdClicked() = channel.invokeMethod("clicked", null)
    override fun onAdImpression() = channel.invokeMethod("impression", null)
    override fun onAdOpened() = channel.invokeMethod("opened", null)
    override fun onAdLeftApplication() = channel.invokeMethod("leftApplication", null)
    override fun onAdClosed() = channel.invokeMethod("closed", null)
  }
}

class AdmobFlutterPlugin(private val registrar: Registrar): MethodCallHandler {
  companion object {
    @JvmStatic
    fun registerWith(reg: Registrar) {
      val defaultChannel = MethodChannel(reg.messenger(), "admob_flutter")
      defaultChannel.setMethodCallHandler(AdmobFlutterPlugin(reg))

      val interstitialChannel = MethodChannel(reg.messenger(), "admob_flutter/interstitial")
      interstitialChannel.setMethodCallHandler(AdmobInterstitial(reg))

      val rewardChannel = MethodChannel(reg.messenger(), "admob_flutter/reward")
      rewardChannel.setMethodCallHandler(AdmobReward(reg))

      reg
        .platformViewRegistry()
        .registerViewFactory("admob_flutter/banner", AdmobBannerFactory(reg.messenger()))
    }
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    when(call.method) {
      "initialize" -> {
        MobileAds.initialize(registrar.context())
        @Suppress("UNCHECKED_CAST")
        (call.arguments as? ArrayList<String>)?.apply {
            val configuration = RequestConfiguration.Builder().setTestDeviceIds(this).build()
            MobileAds.setRequestConfiguration(configuration)
        }
      }
      "banner_size" -> {
        val args = call.arguments as HashMap<*, *>
        val name = args["name"] as String
        val width = args["width"] as Int
        when(name) {
          "SMART_BANNER" -> {
            val metrics = registrar.context().resources.displayMetrics
            result.success(hashMapOf(
                    "width" to AdSize.SMART_BANNER.getWidthInPixels(registrar.context()) / metrics.density,
                    "height" to AdSize.SMART_BANNER.getHeightInPixels(registrar.context()) / metrics.density
            ))
          }
          "ADAPTIVE_BANNER" -> {
            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(registrar.context(), width)
            result.success(hashMapOf(
              "width" to adSize.width,
              "height" to adSize.height
            ))
          }
          else -> result.error("banner_size",  "not implemented name", name)
        }
      }
      else -> result.notImplemented()
    }
  }
}
