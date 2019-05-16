package sncr.service;

import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import org.apache.http.ssl.SSLContextBuilder;
import sncr.saw.common.config.SAWServiceConfig;


/**
 * This generic class can be used as to access internal services
 * @author spau0004
 *
 */
public class InternalServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(InternalServiceClient.class);
    private String url = null;
    public InternalServiceClient(String url) {
        super();
        this.url = url;
    }

    /**
     * Default Constructor
     */
    public InternalServiceClient(){}

    private static CloseableHttpClient client;
    /**
     * This method will be used to access the semantic service to get the
     * details of semantic a particular semantic node
     * @param object
     * @return Object
     */
    public String retrieveObject(Object object) throws Exception {
        logger.trace("request from retrieveObject :"+ object);
        Object node = null;
        ObjectMapper mapper = new ObjectMapper();
        HttpClient client = getHttpClient();
        HttpGet request = new HttpGet(url);
        HttpResponse response = client.execute(request);
        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));
        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        node = mapper.readValue(result.toString(), object.getClass());
        JsonNode jsonNode = mapper.readTree(mapper.writeValueAsString(node));
        ObjectNode rootNode = (ObjectNode) jsonNode;
        logger.trace("response object :" + mapper.writeValueAsString(node));
        return mapper.writeValueAsString(node);
    }

    /**
     * creating a https client.
     */
    public HttpClient getHttpClient() throws Exception {
        if (client != null) {
            return client;
        }
        Boolean sipSslEnable = SAWServiceConfig.sipSsl().getBoolean("enable");
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        if (sipSslEnable) {
            String trustStore = SAWServiceConfig.sipSsl().getString("trust.store");
            String trustStorePassword = SAWServiceConfig.sipSsl().getString("trust.password");
            String keyStore = SAWServiceConfig.sipSsl().getString("key.store");
            String keyStorePassword = SAWServiceConfig.sipSsl().getString("key.password");
            SSLContext sslcontext =
                    getSsLContext(trustStore, trustStorePassword, keyStore, keyStorePassword);
            SSLConnectionSocketFactory factory =
                    new SSLConnectionSocketFactory(sslcontext, new NoopHostnameVerifier());
            client = HttpClients.custom().setSSLSocketFactory(factory).build();
        } else {
            client = HttpClients.custom().setConnectionManager(cm).build();
        }
        return client;
    }


    /**
     * ssl context using store passcode.
     */
    private SSLContext getSsLContext(String trustStore,
                                     String trustPassword, String keyStore, String keyStorePassword) throws KeyStoreException, NoSuchAlgorithmException,
            CertificateException, IOException, KeyManagementException, UnrecoverableKeyException {
        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(new File(trustStore), trustPassword.toCharArray())
                .loadKeyMaterial(new File(keyStore), keyStorePassword.toCharArray(),
                        keyStorePassword.toCharArray())
                .build();
        return sslContext;
    }



}
