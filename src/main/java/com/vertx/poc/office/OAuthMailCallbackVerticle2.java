package com.vertx.poc.office;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.auth.oauth2.Oauth2Credentials;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.StartTLSOptions;
import io.vertx.ext.web.client.WebClient;

import java.nio.file.FileSystems;
import java.time.Duration;
import java.util.List;

public class OAuthMailCallbackVerticle2 extends AbstractVerticle {

  public static final String CURRENT_DIR = System.getProperty("user.dir");
  public static final String PATH_SEPARATOR = FileSystems.getDefault().getSeparator();

  private static final Vertx VERTX = Vertx.vertx(new VertxOptions()
    .setEventLoopPoolSize(20)
    .setWorkerPoolSize(20)
    .setInternalBlockingPoolSize(20)
    .setMaxWorkerExecuteTime(Duration.ofSeconds(600).toNanos())
    .setWarningExceptionTime(Duration.ofSeconds(600).toNanos()));

  // OAuth and SMTP configuration
  private static final String tenantId = ""; // Replace with your tenant ID
  private static final String clientId = ""; // Replace with your client ID
  private static final String clientSecret = ""; // Replace with your client secret
  private static final String redirectUri = ""; // Your redirect URI

  @Override
  public void start() {

    var server = VERTX.createHttpServer(new HttpServerOptions()
      .setUseAlpn(true) // HTTP2 support
      .setSsl(true)
      .setCompressionSupported(true)
      .setMaxWebSocketMessageSize(2 * 1024 * 1024) // 2 MB
      .setPemKeyCertOptions(new PemKeyCertOptions()
        .setCertPath(CURRENT_DIR + PATH_SEPARATOR + "server-cert.pem")
        .setKeyPath(CURRENT_DIR + PATH_SEPARATOR + "server-key.pem")));

    server.requestHandler(req -> {
        String path = req.path();
        if ("/callback".equals(path)) {
          handleCallbackRequest(req);
        } else {
          req.response().setStatusCode(404).end("Not Found");
        }
      })
      .listen(9090, http -> {
        if (http.succeeded()) {
          System.out.println("HTTP Server started on port 9090");
        } else {
          System.err.println("Failed to start HTTP Server: " + http.cause().getMessage());
        }
      });
  }

  private void handleCallbackRequest(HttpServerRequest req) {
    req.bodyHandler(buffer -> {
      // Extract the authorization code from the query parameters
      String code = req.params().get("code");

      if (code != null) {
        System.out.println("Received Authorization Code: " + code);

        // Step 1: Exchange the authorization code for an access token
        OAuth2Options oauth2Options = new OAuth2Options()
          .setFlow(OAuth2FlowType.AUTH_CODE)
          .setClientId(clientId)
          .setClientSecret(clientSecret)
          .setTenant(tenantId)
          .setSite(String.format("https://login.microsoftonline.com/%s", tenantId))
          .setTokenPath("/oauth2/v2.0/token")
          .setAuthorizationPath("/oauth2/v2.0/authorize");

        OAuth2Auth oauth2 = OAuth2Auth.create(vertx, oauth2Options);

        oauth2.authenticate(new Oauth2Credentials()
          .setCode(code)
          .setRedirectUri(redirectUri)
          .setFlow(OAuth2FlowType.AUTH_CODE), authResult -> {
          if (authResult.succeeded()) {
            String accessToken = authResult.result().principal().getString("access_token");

            System.out.println("token access "+accessToken);
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

            WebClient webClient = WebClient.create(VERTX);
            // Send email using Microsoft Graph API
            webClient.post(443, "graph.microsoft.com", "/v1.0/users//sendMail")
              .ssl(true)
              .putHeader("Authorization", "Bearer " + accessToken)
              .putHeader("Content-Type", "application/json")
              .sendJsonObject(emailBody, response -> {
                if (response.succeeded() && response.result().statusCode()==202) {
                  System.out.println("Email sent successfully");
                  req.response().setStatusCode(200).end("success");
                } else {
                  System.err.println("Failed to send email: " +
                    (response.succeeded() ? response.result().bodyAsString():response.cause().getMessage()));
                  req.response().setStatusCode(500).end("fail");
                }
              });


          } else {
            System.err.println("Authentication failed: " + authResult.cause());
            req.response().setStatusCode(500).end("fail");
          }
        });
      } else {
        System.err.println("Authorization code not found in callback request");
        req.response().setStatusCode(400).end("Authorization code missing");
      }
    });
  }

  private void sendEmailWithAccessToken(String accessToken, String username) {
    // Configure MailClient with XOAUTH2
    MailConfig mailConfig = new MailConfig()
      .setHostname("smtp.office365.com")
      .setPort(587)
      .setStarttls(StartTLSOptions.REQUIRED)
      .setAuthMethods("XOAUTH2")
      .setUsername(username) // Replace with your email
      .setPassword(accessToken); // Use the access token as the password for XOAUTH2

    MailClient mailClient = MailClient.create(vertx, mailConfig);

    // Create and send the email
    MailMessage mailMessage = new MailMessage()
      .setFrom("") // Replace with your email
      .setTo(List.of("")) // Replace with recipient email
      .setSubject("Test Subject")
      .setHtml("<h1>Sample Email Content</h1>");

    mailClient.sendMail(mailMessage, result -> {
      if (result.succeeded()) {
        System.out.println("Email sent successfully!");
      } else {
        System.err.println("Failed to send email: " + result.cause().getMessage());
      }
    });
  }

  public static void main(String[] args) {
    Vertx.vertx().deployVerticle(new OAuthMailCallbackVerticle2());
  }
}
