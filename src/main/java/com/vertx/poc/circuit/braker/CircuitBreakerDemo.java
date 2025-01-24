package com.vertx.poc.circuit.braker;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class CircuitBreakerDemo {
  private static final Vertx vertx = Vertx.vertx();
  private final CircuitBreaker circuitBreaker = CircuitBreaker.create("demo-circuit-breaker", vertx,
      new CircuitBreakerOptions()
        .setMaxFailures(4)
        .setMaxRetries(0)
        .setTimeout(5000)
        .setFallbackOnFailure(true)
        .setResetTimeout(5000)    // Reduced to 5 seconds for quicker testing
    )
    .openHandler(v -> {
      System.out.println("[CircuitBreaker] Circuit OPENED!");
    })
    .closeHandler(v -> {
      System.out.println("[CircuitBreaker] Circuit CLOSED!");
    });

//  public CircuitBreakerDemo() {}
//  public CircuitBreakerDemo() {
//    circuitBreaker = CircuitBreaker.create("demo-circuit-breaker", vertx,
//        new CircuitBreakerOptions()
//          .setMaxFailures(4)
//          .setMaxRetries(0)
//          .setTimeout(5000)
//          .setFallbackOnFailure(true)
//          .setResetTimeout(5000)    // Reduced to 5 seconds for quicker testing
//      )
//      .openHandler(v -> {
//        System.out.println("[CircuitBreaker] Circuit OPENED!");
//      })
//      .closeHandler(v -> {
//        System.out.println("[CircuitBreaker] Circuit CLOSED!");
//      });
//      .halfOpenHandler(v -> {
//        System.out.println("[CircuitBreaker] Circuit HALF-OPENED - Testing service...");
//      });
//  }

  public void runDemo() {
    System.out.println("[Demo] Starting CircuitBreaker demo...");
    sendNextRequest(1);
  }

  private void sendNextRequest(final int requestNum) {
    if (requestNum <= 10) {
      System.out.println("\n[Request] Sending request #" + requestNum);

      circuitBreaker.<JsonObject>execute(promise -> {
          vertx.setTimer(500, id -> {  // Added small delay to simulate operation
            simulateOperation(promise, requestNum);
          });
        })
        .onComplete(result -> {
          if (result.succeeded()) {
            System.out.println("[Demo] Request #" + requestNum + " succeeded: " + result.result());
          } else {
            System.out.println("[Demo] Request #" + requestNum + " failed: " + result.cause().getMessage());
          }

          // Schedule next request
          long delay = (requestNum == 6) ? 6000 : 2000;  // Longer delay after request 6 to allow circuit to transition
          vertx.setTimer(delay, id -> sendNextRequest(requestNum + 1));
        });
    } else {
      // Keep the vertx instance running for a while to see all state transitions
      vertx.setTimer(10000, id -> {
        System.out.println("\n[Demo] Demo completed, shutting down...");
        vertx.close();
      });
    }
  }

  private void simulateOperation(Promise<JsonObject> promise, int requestNum) {
    System.out.println("[Operation] Processing request #" + requestNum);

    // Requests 1-2 succeed, 3-6 fail, 7+ succeed
    if (requestNum >= 3 && requestNum <= 6) {
      System.out.println("[Operation] Request #" + requestNum + " failed!");
      promise.fail("Simulated failure for request #" + requestNum);
    } else {
      System.out.println("[Operation] Request #" + requestNum + " succeeded!");
      promise.complete(new JsonObject()
        .put("requestId", requestNum)
        .put("status", "success"));
    }
  }

  public static void main(String[] args) {
    CircuitBreakerDemo demo = new CircuitBreakerDemo();
    demo.runDemo();
  }
}
