package com.vertx.poc.office;

import com.flotomate.common.constants.CommonConstants;
import com.flotomate.common.logger.ServiceLogger;
import com.flotomate.common.rest.UiPasswordEncryptionUtils;
import com.flotomate.common.rest.emailconfig.EmailProvider;
import com.flotomate.common.utils.FlotoStringUtils;
import com.flotomate.models.base.SpringUtility;
import com.flotomate.models.proxy.ProxyAuthenticator;
import com.flotomate.models.proxy.ProxyServer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.auth.http.HttpTransportFactory;
import com.google.auth.oauth2.GoogleCredentials;
import org.apache.commons.collections4.MapUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.*;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

public class Demo {
 public static JSONObject firebaseCredentialMap = new JSONObject();
 private static Logger serviceLogger = LoggerFactory.getLogger(ServiceLogger.class);
 private static Environment environment = SpringUtility.getBean(Environment.class);
 private static String AUTH_TOKEN_URL = "code=%s&grant_type=authorization_code&client_secret=%s&client_id=%s&redirect_uri=%s";
 private static String ACCESS_TOKEN_URL = "grant_type=refresh_token&client_secret=%s&client_id=%s&redirect_uri=%s&refresh_token=%s";
 private static String REDIRECT_URL = "%s/oauth/callback";
 private static String OAUTH_EMAIL_CLIENT_GET_CONFIG_URL = "%s/%s/getconfig?activationCode=%s&r=%s";

 /**
  * Use this method to generate access and refresh token for configuration     *     * @param tokenRequest     * @return
  */
 public static ResponseEntity<Map> oauth2TokenRequest(Oauth2TokenRequestRecord tokenRequest) {
   MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
   String redirectUrl;
   if (tokenRequest.isCustomConfiguration()) {
     redirectUrl = String.format(REDIRECT_URL, tokenRequest.baseUrl());
   } else {
     redirectUrl = FlotoStringUtils.toString(tokenRequest.baseUrl());
   }
   String tokenUrl = tokenRequest.tokenUrl();
   if (tokenRequest.isAuthorizationCodeCall()) {
     tokenUrl = tokenUrl + "?" + String.format(AUTH_TOKEN_URL, tokenRequest.code(), tokenRequest.clientSecret(), tokenRequest.clientId(), redirectUrl);
     body.add("client_id", tokenRequest.clientId());
     body.add("client_secret", tokenRequest.clientSecret());
     body.add(CommonConstants.GRANT_TYPE, "authorization_code");
     body.add("redirect_uri", redirectUrl);
     body.add("code", tokenRequest.code());
   } else {
     tokenUrl = tokenUrl + "?" + String.format(ACCESS_TOKEN_URL, tokenRequest.clientSecret(), tokenRequest.clientId(), redirectUrl, tokenRequest.refreshToken());
     body.add("client_id", tokenRequest.clientId());
     body.add("client_secret", tokenRequest.clientSecret());
     body.add(CommonConstants.GRANT_TYPE, "refresh_token");
     body.add("redirect_uri", redirectUrl);
     body.add("refresh_token", tokenRequest.refreshToken());
   }
   MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
   headers.add("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE);
   HttpEntity<Object> request = new HttpEntity<>(body, headers);
   ResponseEntity<Map> responseEntity = null;
   try {
     responseEntity = tokenRequest.template().postForEntity(new URI(tokenUrl), request, Map.class);
   } catch (RestClientException | URISyntaxException e) {
     serviceLogger.error("Error while access token request", e);
     if (e instanceof HttpClientErrorException.BadRequest) {
       HttpClientErrorException.BadRequest restClientResponseException = (HttpClientErrorException.BadRequest) e;
       HttpHeaders responseHeaders = restClientResponseException.getResponseHeaders();
       if (responseHeaders==null) {
         responseHeaders = new HttpHeaders();
       }
       responseHeaders.add("error", restClientResponseException.getMessage());
       responseHeaders.add("error_description", restClientResponseException.getResponseBodyAsString());
       responseEntity = new ResponseEntity<>(responseHeaders, restClientResponseException.getStatusCode());
     } else {
       HttpHeaders responseHeaders = new HttpHeaders();
       responseHeaders.add("error", e.getMessage());
       responseHeaders.add("error_description", e.getMessage());
       responseEntity = new ResponseEntity<>(responseHeaders, HttpStatus.EXPECTATION_FAILED);
     }
   } catch (Exception e) {
     serviceLogger.error("Error while access token request", e);
     HttpHeaders responseHeaders = new HttpHeaders();
     responseHeaders.add("error", e.getMessage());
     responseHeaders.add("error_description", e.getMessage());
     responseEntity = new ResponseEntity<>(responseHeaders, HttpStatus.EXPECTATION_FAILED);
   }
   return responseEntity;
 }

 /**
  * This method will use to generate access token and refresh token for pre-configured email provider     *     * @param tokenRequestForPreConfiguredProvider     * @return
  */
 public static ResponseEntity<Map> oauth2TokenRequestForPreConfiguredProvider(Oauth2TokenRequestForPreConfiguredProviderRecord tokenRequestForPreConfiguredProvider) {
   String emailClientUrl = environment.getProperty("com.flotomate.email.client.url", "https://email-app.serviceops.ai");
   ResponseEntity<Map> responseEntity = null;
   try {
     emailClientUrl = String.format(OAUTH_EMAIL_CLIENT_GET_CONFIG_URL, emailClientUrl, tokenRequestForPreConfiguredProvider.emailProvider().toValue(), tokenRequestForPreConfiguredProvider.activationCode(), encrypt(tokenRequestForPreConfiguredProvider.activationCode()));
     ResponseEntity<Map> oauthConfigResponse = tokenRequestForPreConfiguredProvider.restTemplate().getForEntity(new URI(emailClientUrl), Map.class);
     Map oauthConfigMap = oauthConfigResponse.getBody();
     if (MapUtils.isNotEmpty(oauthConfigMap)) {
       Oauth2TokenRequestRecord tokenRequest = new Oauth2TokenRequestRecord(tokenRequestForPreConfiguredProvider.code(), FlotoStringUtils.toString(oauthConfigMap.get("clientId")), FlotoStringUtils.toString(oauthConfigMap.get("clientSecret")), FlotoStringUtils.toString(oauthConfigMap.get("tokenUrl")), tokenRequestForPreConfiguredProvider.isAuthorizationCodeCall(), tokenRequestForPreConfiguredProvider.refreshToken(), oauthConfigMap.get("regirectUri"), false, tokenRequestForPreConfiguredProvider.restTemplate());
       responseEntity = oauth2TokenRequest(tokenRequest);
     }
   } catch (RestClientException | URISyntaxException e) {
     serviceLogger.error("Error while access token request", e);
     if (e instanceof HttpClientErrorException.BadRequest) {
       HttpClientErrorException.BadRequest restClientResponseException = (HttpClientErrorException.BadRequest) e;
       HttpHeaders responseHeaders = restClientResponseException.getResponseHeaders();
       if (responseHeaders==null) {
         responseHeaders = new HttpHeaders();
       }
       responseHeaders.add("error", restClientResponseException.getMessage());
       responseHeaders.add("error_description", restClientResponseException.getResponseBodyAsString());
       responseEntity = new ResponseEntity<>(responseHeaders, restClientResponseException.getStatusCode());
     } else {
       HttpHeaders responseHeaders = new HttpHeaders();
       responseHeaders.add("error", e.getMessage());
       responseHeaders.add("error_description", e.getMessage());
       responseEntity = new ResponseEntity<>(responseHeaders, HttpStatus.EXPECTATION_FAILED);
     }
   } catch (Exception e) {
     serviceLogger.error("Error while access token request", e);
     HttpHeaders responseHeaders = new HttpHeaders();
     responseHeaders.add("error", e.getMessage());
     responseHeaders.add("error_description", e.getMessage());
     responseEntity = new ResponseEntity<>(responseHeaders, HttpStatus.EXPECTATION_FAILED);
   }
   return responseEntity;
 }

 public static String getFirebaseAccessTokenForPushNotification(String projectId, String privateKey, String privateKeyId, String clientEmail, String clientId, String clientX509CertUrl, ProxyServer proxyServer) {
   String accessToken = FlotoStringUtils.EMPTY;
   try {
     if (firebaseCredentialMap.isEmpty()) {
       firebaseCredentialMap.put("type", "service_account");
       firebaseCredentialMap.put("project_id", PasswordSecurityUtils.decrypt2way(projectId));
       firebaseCredentialMap.put("private_key_id", PasswordSecurityUtils.decrypt2way(privateKeyId));
       firebaseCredentialMap.put("client_email", PasswordSecurityUtils.decrypt2way(clientEmail));
       firebaseCredentialMap.put("client_id", PasswordSecurityUtils.decrypt2way(clientId));
       firebaseCredentialMap.put("auth_uri", "https://accounts.google.com/o/oauth2/auth");
       firebaseCredentialMap.put("token_uri", "https://oauth2.googleapis.com/token");
       firebaseCredentialMap.put("universe_domain", "googleapis.com");
       firebaseCredentialMap.put("auth_provider_x509_cert_url", "https://www.googleapis.com/oauth2/v1/certs");
       firebaseCredentialMap.put("client_x509_cert_url", PasswordSecurityUtils.decrypt2way(clientX509CertUrl));
       firebaseCredentialMap.put("private_key", PasswordSecurityUtils.decrypt2way(privateKey));
     }
     GoogleCredentials credentials = prepareGoogleCredentials(proxyServer);
     credentials.refreshIfExpired();
     accessToken = credentials.getAccessToken().getTokenValue();
   } catch (Exception e) {
     serviceLogger.error("Error while generating access token for Mobile Push Notification process :", e);
   }
   return accessToken;
 }

 private static GoogleCredentials prepareGoogleCredentials(ProxyServer proxyServer) throws IOException {
   GoogleCredentials credentials = null;
   if (proxyServer!=null) {
     Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyServer.getProxyHost(), proxyServer.getProxyPort()));
     if (FlotoStringUtils.isNotBlank(proxyServer.getUserName()) && FlotoStringUtils.isNotBlank(proxyServer.getPassword())) {
       Authenticator.setDefault(new ProxyAuthenticator(proxyServer.getUserName(), PasswordSecurityUtils.decrypt2way(proxyServer.getPassword())));
     }
     HttpTransportFactory transportFactory = () -> new NetHttpTransport.Builder().setProxy(proxy).build();
     credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(firebaseCredentialMap.toString().getBytes()), transportFactory).createScoped(Collections.singleton("https://www.googleapis.com/auth/cloud-platform"));
   } else {
     credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(firebaseCredentialMap.toString().getBytes())).createScoped(Collections.singleton("https://www.googleapis.com/auth/cloud-platform"));
   }
   return credentials;
 }

 public static String encrypt(String plainText) throws Exception {
   String encryptedBytes = UiPasswordEncryptionUtils.encrypt(plainText);
   return Base64.getUrlEncoder().encodeToString(encryptedBytes.getBytes());
 }

 public record Oauth2TokenRequestForPreConfiguredProviderRecord(String code, String activationCode,
                                                                EmailProvider emailProvider, String refreshToken,
                                                                boolean isAuthorizationCodeCall,
                                                                RestTemplate restTemplate) {
 }

 public record Oauth2TokenRequestRecord(String code, String clientId, String clientSecret, String tokenUrl,
                                        boolean isAuthorizationCodeCall, String refreshToken, Object baseUrl,
                                        boolean isCustomConfiguration, RestTemplate template) {
 }
}
