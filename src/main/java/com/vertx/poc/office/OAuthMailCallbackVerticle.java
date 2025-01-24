package com.vertx.poc.office;

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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.FileSystems;
import java.time.Duration;
import java.util.List;

public class OAuthMailCallbackVerticle extends AbstractVerticle {

  public static final String CURRENT_DIR = System.getProperty("user.dir");
  public static final String PATH_SEPARATOR = FileSystems.getDefault().getSeparator();
  private static final Vertx VERTX = Vertx.vertx(new VertxOptions()
    .setEventLoopPoolSize(20)
    .setWorkerPoolSize(20)
    .setInternalBlockingPoolSize(20)
    .setMaxWorkerExecuteTime(Duration.ofSeconds(600).toNanos())
    .setWarningExceptionTime(Duration.ofSeconds(600).toNanos()));


  private static final String tenantId = "";
  private static final String clientId = "";
  private static final String clientSecret = "";
  private static final String redirectUri = "https://localhost/callback";

  @Override
  public void start() {
    var server = VERTX.createHttpServer(new HttpServerOptions()
      .setUseAlpn(true)
      .setSsl(true)
      .setCompressionSupported(true)
      .setPemKeyCertOptions(new PemKeyCertOptions()
        .setCertPath(CURRENT_DIR + PATH_SEPARATOR + "")
        .setKeyPath(CURRENT_DIR + PATH_SEPARATOR + "")));

    String authUrl = null;
    try {
      authUrl = String.format(
        "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize" +
          "?client_id=%s" +
          "&response_type=code" +
          "&redirect_uri=%s" +
          "&scope=%s" +
          "&state=%s" ,
        tenantId, clientId, redirectUri, "offline_access%20https://outlook.office365.com/SMTP.Send", URLEncoder.encode("wired","UTF-8")
      );

      System.out.println("Authorization URL: " + authUrl);
      System.out.println("Visit the above URL to grant permissions and obtain the authorization code.");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }




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
      String state = req.params().get("state");
      System.out.println("state " + state);

      if (code != null) {
        System.out.println("Received Authorization Code: " + code);


        OAuth2Options oauth2Options = new OAuth2Options()
          .setFlow(OAuth2FlowType.AUTH_CODE)
          .setClientId(clientId)
          .setClientSecret(clientSecret)
//          .setTenant(tenantId)
//          .setSite("https://login.microsoftonline.com//oauth2/v2.0/authorize")
          .setTokenPath("https://login.microsoftonline.com/common/oauth2/v2.0/token")
          .setAuthorizationPath("https://login.microsoftonline.com//oauth2/v2.0/authorize");

        OAuth2Auth oauth2 = OAuth2Auth.create(vertx, oauth2Options);

        oauth2.authenticate(new Oauth2Credentials()
          .setCode(code)
          .setRedirectUri(redirectUri)
          .setFlow(OAuth2FlowType.AUTH_CODE)
          , authResult -> {
          if (authResult.succeeded()) {
            String accessToken = authResult.result().principal().getString("access_token");
            String  username = "";
            System.out.println("Access Token: " + accessToken);
            System.out.println("username: " + username);


            sendEmailWithAccessToken(accessToken,username,req);
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
      .setHostname("smtp.office365.com")
      .setPort(587)
      .setStarttls(StartTLSOptions.REQUIRED)
      .setAuthMethods("XOAUTH2")
      .setUsername(username)
      .setPassword(accessToken)
      .setConnectTimeout(30000);
    MailClient mailClient = MailClient.createShared(vertx, mailConfig);


      MailMessage mailMessage = new MailMessage()
      .setFrom("")
      .setTo(List.of(""))
      .setSubject("Test Subject")
      .setHtml("<h1>Sample Email Content</h1>");

    mailClient.sendMail(mailMessage, result -> {
      if (result.succeeded()) {
        System.out.println("Email sent successfully!");
        req.response().setStatusCode(200).end("Email sent successfully!");
      } else {
        req.response().setStatusCode(500).end("Failed to send email: " + result.cause().getMessage());

      }
    });
  }

  public static void main(String[] args) {
    Vertx.vertx().deployVerticle(new OAuthMailCallbackVerticle());
  }
}
