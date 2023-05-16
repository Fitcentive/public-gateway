package io.fitcentive.public_gateway.domain.errors

import io.fitcentive.sdk.error.DomainError

import java.util.UUID

case class UserServiceError(reason: String) extends DomainError {
  override def code: UUID = UUID.fromString("47ee308e-15b7-4e1e-9a2a-a63e688b7d71")
}
