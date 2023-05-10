package io.fitcentive.public_gateway.domain.ad

import play.api.libs.json.{JsString, Writes}

trait AdType {
  def stringValue: String
}

object AdType {
  def apply(status: String): AdType =
    status match {
      case BannerAdType.stringValue       => BannerAdType
      case InterstitialAdType.stringValue => InterstitialAdType
      case NativeAdType.stringValue       => NativeAdType
      case _                              => throw new Exception("Unexpected auth provider")
    }

  implicit lazy val writes: Writes[AdType] = {
    {
      case BannerAdType       => JsString(BannerAdType.stringValue)
      case NativeAdType       => JsString(NativeAdType.stringValue)
      case InterstitialAdType => JsString(InterstitialAdType.stringValue)
    }
  }

  case object BannerAdType extends AdType {
    val stringValue: String = "banner"
  }

  case object InterstitialAdType extends AdType {
    val stringValue: String = "interstitial"
  }

  case object NativeAdType extends AdType {
    val stringValue: String = "native"
  }

}
