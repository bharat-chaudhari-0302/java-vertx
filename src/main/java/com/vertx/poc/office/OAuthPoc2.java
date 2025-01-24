package com.vertx.poc.office;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.auth.oauth2.Oauth2Credentials;
import io.vertx.ext.auth.oauth2.providers.AzureADAuth;
import io.vertx.ext.web.client.WebClient;

import java.util.List;

public class OAuthPoc2 extends AbstractVerticle {

  @Override
  public void start() {

    String clientId = "";
    String clientSecret = "";
    String tenantId = "";

    // Create web client for direct token request
    WebClient webClient = WebClient.create(vertx);

    // Azure AD configuration
    OAuth2Options config = new OAuth2Options()
      .setClientId(clientId)
      .setClientSecret(clientSecret)
      .setTenant(tenantId);
    config.setSite("https://login.microsoftonline.com/"+tenantId+"/oauth2/v2.0/token");

    AzureADAuth.discover(vertx, config, ar -> {
      if (ar.succeeded()) {
        OAuth2Auth oauth2 = ar.result();

        // Get access token using client credentials flow
        oauth2.authenticate(new Oauth2Credentials().setFlow(OAuth2FlowType.CLIENT)
            .setScopes(List.of("https://graph.microsoft.com/.default"))
          , authResult -> {

            if (authResult.succeeded()) {
              String accessToken = authResult.result().principal().getString("access_token");

              JsonObject emailBody = new JsonObject()
                .put("message", new JsonObject()
                  .put("subject", "Test Subject")
                  .put("body", new JsonObject()
                    .put("contentType", "Text")
                    .put("content", "This is a test email"))
                  .put("toRecipients", new JsonArray()
                    .add(new JsonObject()
                      .put("emailAddress", new JsonObject()
                        .put("address", "")))))
                .put("saveToSentItems", true);



              // Send email using Microsoft Graph API
              webClient.post(443, "graph.microsoft.com", "/v1.0/users//sendMail")
                .ssl(true)
                .putHeader("Authorization", "Bearer " + accessToken)
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(emailBody, response -> {
                  if (response.succeeded() && response.result().statusCode() == 202) {
                    System.out.println("Email sent successfully");
                  } else {
                    System.err.println("Failed to send email: " +
                      (response.succeeded() ? response.result().bodyAsString() : response.cause().getMessage()));
                  }
                  // Close the Vert.x instance after sending
                  vertx.close();
                });


            } else {
              System.err.println("Authentication failed: " + authResult.cause());
              vertx.close();
            }
          });
      } else {
        System.err.println("Failed to initialize Azure AD: " + ar.cause().getMessage());
        vertx.close();
      }
    });


  }

  public static void main(String[] args) {
    Vertx.vertx().deployVerticle(new OAuthPoc2());
  }
}
