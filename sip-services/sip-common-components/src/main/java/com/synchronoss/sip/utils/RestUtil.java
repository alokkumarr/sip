package com.synchronoss.sip.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synchronoss.bda.sip.exception.SipNotProcessedSipEntityException;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Iterator;
import javax.net.ssl.SSLContext;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.owasp.esapi.ESAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

@SuppressWarnings("deprecation")
@Service
public class RestUtil {

  private static final Logger logger = LoggerFactory.getLogger(RestUtil.class);

  @Value("${sip.trust.store:}")
  private String trustStore;

  @Value("${sip.trust.password:}")
  private String trustStorePassword;

  @Value("${sip.key.store:}")
  private String keyStore;

  @Value("${sip.key.password:}")
  private String keyStorePassword;

  @Value("${sip.ssl.enable}")
  private Boolean sipSslEnable;
  
  public static final String sanatizeAndValidateregEx = "[-+.^:,\\\",*,\\\\\\\\,\\[\\]_{}/@!%]";
  public static final String noSpace = "";

  /** creating rest template using SSL connection. */
  public RestTemplate restTemplate() {

    HttpClient client = null;

    logger.trace(
        "ssl enable?"
            + sipSslEnable
            + " restTemplate trustStore: "
            + trustStore
            + " restTemplate keyStore: "
            + keyStore);
    RestTemplate restTemplate = null;
    if (sipSslEnable) {
      SSLContext sslContext = null;
      try {
        sslContext =
            SSLContextBuilder.create()
                .loadKeyMaterial(
                    new File(keyStore),
                    keyStorePassword.toCharArray(),
                    keyStorePassword.toCharArray())
                .loadTrustMaterial(new File(trustStore), trustStorePassword.toCharArray())
                .build();
        client =
            HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(new NoopHostnameVerifier())
                .build();
        HttpComponentsClientHttpRequestFactory factory =
            new HttpComponentsClientHttpRequestFactory(client);
        restTemplate = new RestTemplate(factory);
      } catch (Exception e) {
        logger.error("Exception :" + e);
      }
    } else {
      restTemplate = new RestTemplate();
    }
    return restTemplate;
  }

  /** creating rest template using SSL connection. */
  public RestTemplate restTemplate(
      String keyStore, String keyPassword, String trustStore, String trustPassword) {
    HttpClient client = null;

    logger.trace(
        "ssl enable?"
            + sipSslEnable
            + " restTemplate trustStore: "
            + trustStore
            + " restTemplate keyStore: "
            + keyStore);

    RestTemplate restTemplate = null;
    if (sipSslEnable) {
      SSLContext sslContext = null;
      try {
        sslContext =
            SSLContextBuilder.create()
                .loadKeyMaterial(
                    new File(keyStore), keyPassword.toCharArray(), keyPassword.toCharArray())
                .loadTrustMaterial(new File(trustStore), trustPassword.toCharArray())
                .build();
        client =
            HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(new NoopHostnameVerifier())
                .build();
        HttpComponentsClientHttpRequestFactory factory =
            new HttpComponentsClientHttpRequestFactory(client);
        restTemplate = new RestTemplate(factory);
      } catch (Exception e) {
        logger.error("Exception :" + e);
      }
    } else {
      restTemplate = new RestTemplate();
    }
    return restTemplate;
  }

  /**
   * creating async rest template using SSL connection. TODO: This method should be changed when
   * AsyncRestTemplate changes to WebClient
   */
  public AsyncRestTemplate asyncRestTemplate() {
    logger.trace(
        "ssl enable?"
            + sipSslEnable
            + " restTemplate trustStore: "
            + trustStore
            + " restTemplate keyStore: "
            + keyStore);
    AsyncRestTemplate restTemplate = null;
    if (sipSslEnable) {
      SSLContext sslContext = null;
      try {
        sslContext =
            SSLContextBuilder.create()
                .loadKeyMaterial(
                    new File(keyStore),
                    keyStorePassword.toCharArray(),
                    keyStorePassword.toCharArray())
                .loadTrustMaterial(new File(trustStore), trustStorePassword.toCharArray())
                .build();
      } catch (Exception e) {
        logger.error("Error occured while building SSL context", e);
      }

      CloseableHttpAsyncClient httpclient =
          HttpAsyncClients.custom()
              .setSSLHostnameVerifier(new NoopHostnameVerifier())
              .setSSLContext(sslContext)
              .build();
      AsyncClientHttpRequestFactory reqFactory =
          new HttpComponentsAsyncClientHttpRequestFactory(httpclient);
      restTemplate = new AsyncRestTemplate(reqFactory);
    } else {
      restTemplate = new AsyncRestTemplate();
    }
    return restTemplate;
  }

  /** creating a https client. */
  public HttpClient getHttpClient() throws Exception {
    logger.trace(
        "ssl enable?"
            + sipSslEnable
            + " restTemplate trustStore: "
            + trustStore
            + " restTemplate keyStore: "
            + keyStore);
    HttpClient client = null;
    PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
    if (sipSslEnable) {
      SSLContext sslcontext =
          getSsLContext(keyStore, keyStorePassword, trustStore, trustStorePassword);
      SSLConnectionSocketFactory factory =
          new SSLConnectionSocketFactory(sslcontext, new NoopHostnameVerifier());
      client = HttpClients.custom().setSSLSocketFactory(factory).build();
    } else {
      client = HttpClients.custom().setConnectionManager(cm).build();
    }
    return client;
  }

  /** ssl context using store passcode. */
  private SSLContext getSsLContext(
      String keyStore, String keyPassword, String trustStore, String trustPassword)
      throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException,
          KeyManagementException, UnrecoverableKeyException {
    logger.trace(
        "ssl enable?"
            + sipSslEnable
            + " restTemplate trustStore: "
            + trustStore
            + " restTemplate keyStore: "
            + keyStore);

    SSLContext sslContext =
        SSLContextBuilder.create()
            .loadKeyMaterial(
                new File(keyStore), keyPassword.toCharArray(), keyPassword.toCharArray())
            .loadTrustMaterial(new File(trustStore), trustPassword.toCharArray())
            .build();
    return sslContext;
  }

  /** getTrustStore. */
  public String getTrustStore() {
    return trustStore;
  }

  /** getTrustStorePassword. */
  public String getTrustStorePassword() {
    return trustStorePassword;
  }

  /** getKeyStore. */
  public String getKeyStore() {
    return keyStore;
  }

  /** getKeyStorePassword. */
  public String getKeyStorePassword() {
    return keyStorePassword;
  }

  /** getTrustStore. */
  public Boolean getSipSslEnable() {
    return sipSslEnable;
  }
  
  /**
   * Thsi method traverse the node & validates the value.
   * @param parentNode {@link JsonNode}
   * @throws IOException {@link IOException}
   */
  public static void validateNodeValue(JsonNode parentNode) throws IOException {
    Boolean isValid = Boolean.TRUE;
    String nodeName = null;
    String nodeText = null;
    if (parentNode.isArray()) {
      Iterator<JsonNode> iter = parentNode.elements();
      while (iter.hasNext()) {
        JsonNode node = iter.next();
        if (node.isObject() || node.isArray()) {
          validateNodeValue(node);
        }
      }
    }
    if (parentNode.isObject()) {
      Iterator<String> iter = parentNode.fieldNames();
      while (iter.hasNext()) {
        nodeName = iter.next();
        JsonNode node = parentNode.path(nodeName);
        if (node.isObject() || node.isArray()) {
          validateNodeValue(node);
        } else {
          nodeText = node.asText().replaceAll(sanatizeAndValidateregEx,noSpace);
          // Validating for ESAPI constraints
          isValid = ESAPI.validator().isValidInput("Validating SafeString "
              + "attributes value for intrusion",
              nodeText, "SafeString", node.asText().toString().length(), false);
          
          if (!isValid) {
            isValid =
                ESAPI.validator().isValidInput("Validating Email attributes value for intrusion",
                    nodeText, "Email", node.asText().toString().length(), false);
            if (!isValid) {
              isValid =
                  ESAPI.validator().isValidInput("Validating Password "
                      + "attributes value for intrusion",
                      nodeText, "Password", node.asText().toString().length(), false);
            }
            if (!isValid) {
              isValid =
                  ESAPI.validator().isValidInput("Validating IPAddress "
                      + "attributes value for intrusion",
                      nodeText, "IPAddress", node.asText().toString().length(), false);
            }
            if (!isValid) {
              isValid =
                  ESAPI.validator().isValidInput("Validating URL "
                      + "attributes value for intrusion",
                      nodeText, "URL", node.asText().toString().length(), false);
            }
            if (!isValid) {
              isValid =
                  ESAPI.validator().isValidInput("Validating Id "
                      + "attributes value for intrusion",
                      nodeText, "Id", node.asText().toString().length(), false);
            }
            if (!isValid) {
              isValid =
                  ESAPI.validator().isValidInput("Validating SafeText "
                      + "attributes value for intrusion",
                      nodeText, "SafeText", node.asText().toString().length(), false);
            }
            if (!isValid) {
              logger.trace("Attribute is of type Json");
              ObjectMapper m = new ObjectMapper();
              JsonNode rootNode = m.readTree(m.writeValueAsString(nodeText));
              validateNodeValue(rootNode);
            }
          }
        }
      }
    }
    if (!isValid) {
      throw new SipNotProcessedSipEntityException(
          nodeName + ":'" + nodeText + "' is not valid");
    }
    
  }


}
