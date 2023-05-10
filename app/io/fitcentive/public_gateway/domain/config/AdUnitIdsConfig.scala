package io.fitcentive.public_gateway.domain.config

import com.typesafe.config.Config

case class AdUnitIdsConfig(iosAdUnitId: String, androidAdUnitId: String)

object AdUnitIdsConfig {
  def fromConfig(config: Config): AdUnitIdsConfig =
    AdUnitIdsConfig(iosAdUnitId = config.getString("ios"), androidAdUnitId = config.getString("android"))
}
