package io.fitcentive.public_gateway.services

import com.google.inject.ImplementedBy
import io.fitcentive.public_gateway.infrastructure.pubsub.EventPublisherService

import java.util.UUID
import scala.concurrent.Future

@ImplementedBy(classOf[EventPublisherService])
trait MessageBusService {
  def publishEnablePremiumForUser(userId: UUID): Future[Unit]
  def publishDisablePremiumForUser(userId: UUID): Future[Unit]
}
