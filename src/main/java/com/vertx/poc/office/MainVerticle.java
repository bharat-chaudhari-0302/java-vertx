package com.vertx.poc.office;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

import java.time.Duration;

public class MainVerticle extends AbstractVerticle {

  private static final Vertx VERTX = Vertx.vertx(new VertxOptions().setEventLoopPoolSize(20)
    .setWorkerPoolSize(20)
    .setInternalBlockingPoolSize(20)
    .setMaxWorkerExecuteTime(Duration.ofSeconds(600).toNanos())
    .setWarningExceptionTime(Duration.ofSeconds(600).toNanos()));

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    VERTX.deployVerticle(OAuthPoc.class.getName());

  }
}
