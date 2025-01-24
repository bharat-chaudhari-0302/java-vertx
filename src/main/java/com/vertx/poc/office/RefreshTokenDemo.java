package com.vertx.poc.office;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.impl.UserImpl;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2Options;

public class RefreshTokenDemo extends AbstractVerticle {

  public static JsonObject authContext = new JsonObject();

  @Override
  public void init(Vertx vertx, Context context) {
    authContext = new JsonObject("{\"authentication.type\": \"OAuth 2.0\",\"client.id\": \"\",\"authentication.url\": \"https://login.microsoftonline.com//oauth2/v2.0/authorize\",\"redirect.uri\": \"https://10.20.40.117/callback\",\"token.url\": \"https://login.microsoftonline.com/common/oauth2/v2.0/token\",\"scopes\": \"offline_access%20https://outlook.office365.com/SMTP.Send\",\"client.secret\": \"\",\"unique.id\": \"4120fba9-e996-4128-8751-1a736e1b26da\",\"token_type\": \"Bearer\",\"scope\": \"https://outlook.office365.com/IMAP.AccessAsUser.All https://outlook.office365.com/SMTP.Send\",\"expires_in\": 3981,\"ext_expires_in\": 3981,\"access_token\": \"ey...\",\"refresh_token\": \"1.Ab...\"}");

    vertx.setPeriodic(3 * 1000, sample -> {
      try {
        String accessToken = fetchValidTokenBlocking(authContext, vertx);
        System.out.println("Result: " + accessToken);
      } catch (Exception e) {
        System.err.println("Error fetching token: " + e.getMessage());
      }
    });
  }

  public static String fetchValidTokenBlocking(JsonObject context, Vertx vertx) throws Exception {
    // Use the asynchronous fetchValidToken method and wait for its result
    return fetchValidToken(context, vertx)
      .toCompletionStage()  // Convert Future to CompletionStage
      .toCompletableFuture()  // Convert to CompletableFuture
      .get(); // Blocking wait
  }

  public static Future<String> fetchValidToken(JsonObject context, Vertx vertx) {
    var promise = Promise.<String>promise();

    // Check if expires_at is set and valid
    if (context.containsKey("expires_at")) {
      long expiresAt = context.getLong("expires_at");
      long currentTime = System.currentTimeMillis();
      if (currentTime < expiresAt) {
        // Token is still valid
        System.out.println("Token is still valid, using the current access token.");
        promise.complete(context.getString("access_token"));
        return promise.future();
      }
    }

    // Token is expired or expires_at is not set, refresh the token
    System.out.println("Token expired or not set, refreshing...");
    var oauth2Options = new OAuth2Options()
      .setClientId(context.getString("client.id"))
      .setClientSecret(context.getString("client.secret"))
      .setTokenPath(context.getString("token.url"))
      .setAuthorizationPath(context.getString("authentication.url"));

    var oauth2 = OAuth2Auth.create(vertx, oauth2Options);

    if (context.containsKey("refresh_token") && !context.getString("refresh_token").trim().isEmpty()) {
      User user = new UserImpl(new JsonObject(), new JsonObject());

      JsonObject principal = new JsonObject()
        .put("refresh_token", context.getString("refresh_token"));

      user.principal().mergeIn(principal);

      oauth2.refresh(user).onSuccess(response -> {
        String accessToken = response.principal().getString("access_token");
        int expiresIn = response.principal().getInteger("expires_in"); // Expires in seconds

        // Calculate expires_at in milliseconds and update the context
        long expiresAt = System.currentTimeMillis() + expiresIn * 1000L;
        context.put("access_token", accessToken)
          .put("expires_at", expiresAt);

        System.out.println("Access token refreshed successfully.");
        promise.complete(accessToken);
      }).onFailure(error -> {
        System.out.println("Failed to refresh access token: " + error.getMessage());
        promise.fail(error);
      });
    } else {
      promise.fail("Refresh token is required but missing.");
      System.out.println("Refresh token is required but missing.");
    }

    return promise.future();
  }

  public static void main(String[] args) {
    Vertx.vertx().deployVerticle(new RefreshTokenDemo());
  }
}
