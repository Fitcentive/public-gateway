package io.fitcentive.public_gateway.infrastructure.pubsub

import io.fitcentive.public_gateway.domain.config.TopicsConfig
import io.fitcentive.public_gateway.infrastructure.contexts.PubSubExecutionContext
import io.fitcentive.public_gateway.services.{MessageBusService, SettingsService}
import io.fitcentive.registry.events.user.{UserDisablePremium, UserEnablePremium}
import io.fitcentive.sdk.gcp.pubsub.PubSubPublisher

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scala.util.chaining.scalaUtilChainingOps

@Singleton
class EventPublisherService @Inject() (publisher: PubSubPublisher, settingsService: SettingsService)(implicit
  ec: PubSubExecutionContext
) extends MessageBusService {

  private val publisherConfig: TopicsConfig = settingsService.pubSubConfig.topicsConfig

  override def publishEnablePremiumForUser(userId: UUID): Future[Unit] =
    UserEnablePremium(userId)
      .pipe(publisher.publish(publisherConfig.userEnablePremiumTopic, _))

  override def publishDisablePremiumForUser(userId: UUID): Future[Unit] =
    UserDisablePremium(userId)
      .pipe(publisher.publish(publisherConfig.userDisablePremiumTopic, _))

}
