package com.vertx.poc.office;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.auth.oauth2.Oauth2Credentials;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.web.client.WebClient;

import java.util.List;

public class OAuthPoc4 extends AbstractVerticle {

  public static void main(String[] args) {
    Vertx.vertx().deployVerticle(new OAuthPoc4());
  }

  @Override
  public void start() {

    String clientId = "";
    String clientSecret = "";
    String tenantId = "";

    // Create web client for direct token request
    WebClient webClient = WebClient.create(vertx);

    OAuth2Options credentials = new OAuth2Options()
      .setFlow(OAuth2FlowType.CLIENT)
      .setClientId(clientId)
      .setClientSecret(clientSecret)
      .setAuthorizationPath("/oauth2/v2.0/authorize")
      .setTokenPath("/oauth2/v2.0/token")
      .setRevocationPath("/oauth2/v2.0/revoke")
      .setSite("https://login.microsoftonline.com/" + tenantId );


    OAuth2Auth oauth2 = OAuth2Auth.create(vertx, credentials);


    // Get access token using client credentials flow
    oauth2.authenticate(new Oauth2Credentials().setFlow(OAuth2FlowType.CLIENT)
        .setScopes(List.of("https://graph.microsoft.com/.default"))
      , authResult -> {

        if (authResult.succeeded()) {
          String accessToken = authResult.result().principal().getString("access_token");
          System.out.println("token =>  "+accessToken);
          // Configure MailClient with XOAUTH2
          MailConfig mailConfig = new MailConfig()

            .setHostname("smtp.office365.com")
            .setPort(587)
//            .setStarttls(StartTLSOptions.REQUIRED)
            .setAuthMethods("XOAUTH2")
            .setUsername("")
            .setPassword(accessToken); // Access Token as "password"

          MailClient mailClient = MailClient.create(vertx, mailConfig);

          // Create MailMessage
          MailMessage mailMessage = new MailMessage()
            .setFrom("")
            .setTo(List.of(""))
            .setSubject("Test Subject")
            .setHtml("<h1>Sample Mail Content</h1>");

          // Send Mail
          mailClient.sendMail(mailMessage, result -> {
            if (result.succeeded()) {
              System.out.println("Email sent successfully!");
            } else {
              System.err.println("Failed to send email: " + result.cause().getMessage());
            }
            vertx.close();
          });

        } else {
          System.err.println("Authentication failed: " + authResult.cause());
          vertx.close();
        }
      });


  }
}
