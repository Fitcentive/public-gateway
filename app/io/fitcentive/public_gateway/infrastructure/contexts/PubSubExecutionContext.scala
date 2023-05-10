package io.fitcentive.public_gateway.infrastructure.contexts

import play.api.libs.concurrent.CustomExecutionContext

import javax.inject.{Inject, Singleton}

@Singleton
class PubSubExecutionContext @Inject() (actorSystem: akka.actor.ActorSystem)
  extends CustomExecutionContext(actorSystem, "contexts.pubsub-execution-context")
