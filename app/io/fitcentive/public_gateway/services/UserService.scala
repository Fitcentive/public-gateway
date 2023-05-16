package io.fitcentive.public_gateway.services

import com.google.inject.ImplementedBy
import io.fitcentive.public_gateway.domain.user.User
import io.fitcentive.public_gateway.infrastructure.rest.RestUserService
import io.fitcentive.sdk.error.DomainError

import java.util.UUID
import scala.concurrent.Future

@ImplementedBy(classOf[RestUserService])
trait UserService {
  def getUser(userId: UUID): Future[Either[DomainError, User]]
}
