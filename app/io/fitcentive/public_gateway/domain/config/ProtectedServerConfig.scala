package io.fitcentive.public_gateway.domain.config

import com.typesafe.config.Config

case class ProtectedServerConfig(host: String, port: String, token: String) {
  val serverUrl: String = s"$host:$port"
}

object ProtectedServerConfig {
  def fromConfig(config: Config): ProtectedServerConfig =
    ProtectedServerConfig(
      host = config.getString("host"),
      port = config.getString("port"),
      token = config.getString("token")
    )
}
