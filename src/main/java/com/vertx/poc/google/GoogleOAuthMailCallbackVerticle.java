package com.vertx.poc.google;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.auth.oauth2.Oauth2Credentials;
import io.vertx.ext.mail.*;

import java.nio.file.FileSystems;
import java.time.Duration;
import java.util.List;

public class GoogleOAuthMailCallbackVerticle extends AbstractVerticle {

  public static final String CURRENT_DIR = System.getProperty("user.dir");
  public static final String PATH_SEPARATOR = FileSystems.getDefault().getSeparator();
  private static final Vertx VERTX = Vertx.vertx(new VertxOptions()
    .setEventLoopPoolSize(20)
    .setWorkerPoolSize(20)
    .setInternalBlockingPoolSize(20)
    .setMaxWorkerExecuteTime(Duration.ofSeconds(600).toNanos())
    .setWarningExceptionTime(Duration.ofSeconds(600).toNanos()));


  private static final String clientId = "";
  private static final String clientSecret = "";
  private static final String redirectUri = "";

  @Override
  public void start() {
    var server = VERTX.createHttpServer(new HttpServerOptions()
      .setUseAlpn(true)
      .setSsl(true)
      .setCompressionSupported(true)
      .setMaxWebSocketMessageSize(2 * 1024 * 1024) // 2 MB
      .setPemKeyCertOptions(new PemKeyCertOptions()
        .setCertPath(CURRENT_DIR + PATH_SEPARATOR + "")
        .setKeyPath(CURRENT_DIR + PATH_SEPARATOR + "")));

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

      String code = req.params().get("code");
      String someId = req.params().get("someId");
      System.out.println("someId " + someId);
      if (code != null) {
        System.out.println("Received Authorization Code: " + code);


        OAuth2Options oauth2Options = new OAuth2Options()
          .setFlow(OAuth2FlowType.AUTH_CODE)
          .setClientId(clientId)
          .setClientSecret(clientSecret)
          .setSite("https://accounts.google.com")
          .setTokenPath("/o/oauth2/token")
          .setAuthorizationPath("/o/oauth2/auth");

        OAuth2Auth oauth2 = OAuth2Auth.create(vertx, oauth2Options);

        oauth2.authenticate(new Oauth2Credentials()
          .setCode(code)
          .setRedirectUri(redirectUri)
          .setFlow(OAuth2FlowType.AUTH_CODE), authResult -> {
          if (authResult.succeeded()) {
            String accessToken = authResult.result().principal().getString("access_token");
            String username = "";
            System.out.println("Access Token: " + authResult.result().principal());
            System.out.println("Username: " + username);


            sendEmailWithAccessToken(accessToken, username,req);
          } else {
            System.err.println("Failed to authenticate: " + authResult.cause().getMessage());
            req.response().setStatusCode(500).end("Authentication failed");
          }
        });
      } else {
        System.err.println("Authorization code not found in callback request");
        req.response().setStatusCode(400).end("Authorization code missing");
      }
    });
  }

  private void sendEmailWithAccessToken(String accessToken, String username, HttpServerRequest req) {

    MailConfig mailConfig = new MailConfig()
      .setHostname("smtp.gmail.com")
      .setPort(587)
      .setStarttls(StartTLSOptions.REQUIRED)
      .setAuthMethods("XOAUTH2")
      .setUsername(username)
      .setPassword(accessToken)
      .setConnectTimeout(30000);
    MailClient mailClient = MailClient.create(vertx, mailConfig);


    MailMessage mailMessage = new MailMessage()
      .setFrom(username)
      .setTo(List.of(""))
      .setSubject("Test Subject")
      .setHtml("<h1>Sample Email Content</h1>");

    mailClient.sendMail(mailMessage, result -> {
      if (result.succeeded()) {
        System.out.println("Email sent successfully!");
        req.response().setStatusCode(400).end("Email sent successfully!");
      } else {
        System.err.println("Failed to send email: " + result.cause().getMessage());
        req.response().setStatusCode(400).end("Failed to send email: " + result.cause().getMessage());
      }
    });
  }

  public static void main(String[] args) {
    Vertx.vertx().deployVerticle(new GoogleOAuthMailCallbackVerticle());
  }
}
