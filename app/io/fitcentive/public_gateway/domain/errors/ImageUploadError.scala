package io.fitcentive.public_gateway.domain.errors

import io.fitcentive.sdk.error.DomainError

import java.util.UUID

case class ImageUploadError(reason: String) extends DomainError {
  override def code: UUID = UUID.fromString("cdc90c5d-354d-430f-b16a-dace9c06c3ad")
}
