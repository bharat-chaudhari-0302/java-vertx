package com.vertx.poc.playground;

import io.vertx.core.*;

public class BlockingExample {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    // Simulate an async operation with Promise and Future
    Future<String> future = simulateAsyncOperation(vertx);

    try {
      // Blocking wait for the Future to complete
      String result = future.toCompletionStage().toCompletableFuture().get();
      System.out.println("Operation completed with result: " + result);
    } catch (Exception e) {
      System.err.println("Operation failed: " + e.getMessage());
    }

    vertx.close(); // Shutdown Vert.x instance
  }

  private static Future<String> simulateAsyncOperation(Vertx vertx) {
    Promise<String> promise = Promise.promise();

    // Simulate an async task that takes 2 seconds
    vertx.setTimer(2000, id -> {
      System.out.println("Async operation is done.");
      promise.complete("Success");
    });

    return promise.future();
  }
}
