package com.vertx.poc.office;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.auth.oauth2.Oauth2Credentials;
import io.vertx.ext.mail.*;

import java.time.Duration;
import java.util.List;

public class OAuthPoc extends AbstractVerticle {

  private static final Vertx VERTX = Vertx.vertx(new VertxOptions()
    .setEventLoopPoolSize(20)
    .setWorkerPoolSize(20)
    .setInternalBlockingPoolSize(20)
    .setMaxWorkerExecuteTime(Duration.ofSeconds(600).toNanos())
    .setWarningExceptionTime(Duration.ofSeconds(600).toNanos()));

  private static final String tenantId = "";
  private static final String clientId = "";
  private static final String clientSecret = "";
  private static final String redirectUri = "";

  @Override
  public void start() {
    // Azure AD configuration
//    String tenantId = ""; // Replace with your tenant ID
//    String clientId = ""; // Replace with your client ID
//    String clientSecret = ""; // Replace with your client secret
//    String redirectUri = ""; // Ensure this matches what you register in Azure

    // Step 1: Construct the authorization URL
    String authUrl = String.format(
      "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize" +
        "?client_id=%s" +
        "&response_type=code" +
        "&redirect_uri=%s" +
        "&scope=%s",
      tenantId, clientId, redirectUri, "openid%20offline_access%20https://outlook.office365.com/IMAP.AccessAsUser.All"
    );



    System.out.println("Authorization URL: " + authUrl);
    System.out.println("Visit the above URL to grant permissions and obtain the authorization code.");


    String code = "AUoAw85KW5Ilh0GUiZjGVMxsh8WyO87Gv2hBiDQUc28TWWsuAXJKAA.AgABBAIAAADW6jl31mB3T7ugrWTT8pFeAwDs_wUA9P_qELViV6WbQgh7iAkFSURAXr2Qh8yQXdNpgheVIYLEYeBbEJhkdjKOn8b-EZfJpnOqfWqBBArvfpOSoK_v7zHFh5QlSzOwynccFFQJLUtzLxh5vjqyX-uObeUS8wDYUWd4hDjw9p6LJaTwjjnJVxrQltg7VjTd5VBS6YC75bMIafOdl4PGvL6am6tB-ymIwfPqlRYkDSxaum8dNIuvNDnxzRLelGLc-yfuNF41QIjGa9HCKBx3YEp7byQ4g_rQys5xKjqfLEuFhNbNPNTYOdz-kC2H0nJDrChsNbQZUfm6K3XJOSu5zDpsJ2vpliH-H-UoxU4v9eaxrdTMufjN5fUHvbDYKEzrrSZz0cnmMG4NgLGisF67ghMaEGynPrfm3E0bAVo5-fg8Qd-HSnSlpf6GqZZdbf-sK2TKqzz4ONMQhHst6SmYwuhiuZmS-5QSflAiZ5tJB14gk6G7jGTTzrBqN2ScPXWHpXJraS6ec_98dgmGFq7lFwFCAKBgBfjlZpBW-VkqcN-jT8RXIvzuK1nPt-TcA4SN1aUcJmWDpqR-HV8TXAzZICNtL2GD835qaXfRqOiCGHoQQkDpEXC2cdDtKxN2neKVxAwdMuwI5IQURW20UkpBzkwDnIxpZOrB8_ZZ1Bu4fXIdjPrsGbkNHY5yJKXHDGeAHPRKrj-UtKARp6a9AnpOcXbTVByOc2j1PzScAKML-KwK0p5XVKFg22Lj9hnrNt8G0JEVZqdBDMDFCTeuwguqfDW-gPsaKjk7eVhDlLIWKDw&session_state=49e9a0d8-e689-4362-95d7-2bc7590b3d04";
    if (code != null) {
      System.out.println("Authorization Code: " + code);

      // Step 3: Exchange the authorization code for tokens
      OAuth2Options config = new OAuth2Options()
        .setFlow(OAuth2FlowType.AUTH_CODE)
        .setClientId(clientId)
        .setClientSecret(clientSecret)
        .setTenant(tenantId)
        .setSite(String.format("https://login.microsoftonline.com/%s", tenantId))
        .setTokenPath("/oauth2/v2.0/token")
        .setAuthorizationPath("/oauth2/v2.0/authorize");

      OAuth2Auth oauth2 = OAuth2Auth.create(vertx, config);

      oauth2.authenticate(
        new Oauth2Credentials()
          .setCode(code)
          .setRedirectUri(redirectUri)
          .setFlow(OAuth2FlowType.AUTH_CODE),
        authResult -> {
          if (authResult.succeeded()) {
            String accessToken = authResult.result().principal().getString("access_token");
            System.out.println("Access Token: " + accessToken);

            // Configure MailClient with XOAUTH2
            MailConfig mailConfig = new MailConfig()
              .setHostname("smtp.office365.com")
              .setPort(587)
              .setStarttls(StartTLSOptions.REQUIRED)
              .setAuthMethods("XOAUTH2")
              .setUsername("")
              .setPassword(accessToken); // Access Token as "password"

            MailClient mailClient = MailClient.create(vertx, mailConfig);

            // Create and send email
            MailMessage mailMessage = new MailMessage()
              .setFrom("")
              .setTo(List.of(""))
              .setSubject("Test Subject")
              .setHtml("<h1>Sample Mail Content</h1>");

            mailClient.sendMail(mailMessage, result -> {
              if (result.succeeded()) {
                System.out.println("Email sent successfully!");
              } else {
                System.err.println("Failed to send email: " + result.cause().getMessage());
              }

              // Close Vert.x only after completing all operations
              vertx.close(ar -> {
                if (ar.failed()) {
                  System.err.println("Failed to close Vert.x: " + ar.cause().getMessage());
                }
              });
            });

          } else {
            System.err.println("Failed to authenticate: " + authResult.cause().getMessage());
            // Don't close Vert.x here if the app needs to retry
          }
        }
      );

      System.out.println("Authorization code received. Check the console for further steps.");
    } else {
      System.out.println("Authorization code not found.");
    }
  }

  public static void main(String[] args) {
    VERTX.deployVerticle(OAuthPoc.class.getName());
  }
}
