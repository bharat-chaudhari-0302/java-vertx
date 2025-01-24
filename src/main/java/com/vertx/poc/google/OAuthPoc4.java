package com.vertx.poc.google;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.auth.oauth2.Oauth2Credentials;
import io.vertx.ext.auth.oauth2.providers.GoogleAuth;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.StartTLSOptions;
import io.vertx.ext.web.client.WebClient;

import java.time.Duration;
import java.util.List;

public class OAuthPoc4 extends AbstractVerticle
{

  public static void main(String[] args)
  {
    Vertx.vertx().deployVerticle(new OAuthPoc4());
  }

  @Override
  public void start() {

    String clientId = "";
    String clientSecret = "";

    // Azure AD configuration
    OAuth2Options config = new OAuth2Options()
      .setClientId(clientId)
      .setClientSecret(clientSecret);

    GoogleAuth.discover(vertx,config,asyncResult ->
    {
      if (asyncResult.succeeded())
      {
        OAuth2Auth oauth2 = asyncResult.result();

        // Get access token using client credentials flow
        oauth2.authenticate(
          new Oauth2Credentials().setFlow(OAuth2FlowType.AUTH_CODE).setScopes(List.of("https://www.googleapis.com/auth/gmail.send")),
          authResult ->
          {
            if (authResult.succeeded())
            {
              String accessToken = authResult.result().principal().getString("access_token");

              System.out.println("token =>  "+accessToken);

            }
            else
            {
              System.err.println("Authentication failed: " + authResult.cause());
              vertx.close();
            }
          });
      }
      else
      {
        System.out.println("failed to discover google auth : "+ asyncResult.cause());
      }

    });

  }
}
