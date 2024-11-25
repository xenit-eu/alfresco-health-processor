package eu.xenit.alfresco.healthprocessor.plugins.solr.utils;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.ssl.SSLContexts;
import org.springframework.util.ResourceUtils;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Properties;

/***
 * Factory to create Basic or MTLS http clients used by the plugin
 *
 */

@Slf4j
public class HealthProcessorSimpleHttpClientFactory {

    private Properties globalProperties;

    //Note that some of these are alfresco variables.
    private static final String GLOBAL_PROPERTY_PLUGIN_KEY_PREFIX = "eu.xenit.alfresco.healthprocessor.plugin.solr-index.";
    public static final String GLOBAL_PROPERTY_SOLRREQUESTEXECUTOR_USE_SSL = GLOBAL_PROPERTY_PLUGIN_KEY_PREFIX.concat("use-ssl");
    public static final String GLOBAL_PROPERTY_TRUSTSTORE_FILE_LOCATION = "encryption.ssl.truststore.location";
    public static final String GLOBAL_PROPERTY_KEYSTORE_TYPE = GLOBAL_PROPERTY_PLUGIN_KEY_PREFIX.concat("keystore.type");
    public static final String GLOBAL_PROPERTY_KEYSTORE_PASSWORD = GLOBAL_PROPERTY_PLUGIN_KEY_PREFIX.concat("keystore.password");
    public static final String GLOBAL_PROPERTY_KEYSTORE_FILE_LOCATION = "encryption.ssl.keystore.location";
    public static final String GLOBAL_PROPERTY_TRUSTSTORE_TYPE = GLOBAL_PROPERTY_PLUGIN_KEY_PREFIX.concat("truststore.type");
    public static final String GLOBAL_PROPERTY_TRUSTSTORE_PASSWORD = GLOBAL_PROPERTY_PLUGIN_KEY_PREFIX.concat("truststore.password");
    public static final String GLOBAL_PROPERTY_HEALTHPROCESSOR_VALIDATE_HOSTNAMES = "SSL-validate-hostnames";

    HealthProcessorSimpleHttpClientFactory(Properties globalProperties) {
        this.globalProperties = globalProperties;
    }

    public HttpClient createHttpClient() {
        boolean useSSLClient =  Boolean.parseBoolean(globalProperties.getProperty(GLOBAL_PROPERTY_SOLRREQUESTEXECUTOR_USE_SSL));

        if (useSSLClient) {
            return createSslHttpClient();
        }
        else
        {
            return HttpClientBuilder.create().build();
        }
    }

    public HttpClient createSslHttpClient() {

        log.debug("Creating MTLS httpclient");
        log.debug("- keystore location: {}", globalProperties.getProperty(GLOBAL_PROPERTY_KEYSTORE_FILE_LOCATION));
        log.debug("- keystore type: {}", globalProperties.getProperty(GLOBAL_PROPERTY_KEYSTORE_TYPE));
        log.debug("- truststore location: {}", globalProperties.getProperty(GLOBAL_PROPERTY_TRUSTSTORE_FILE_LOCATION));
        log.debug("- truststore type: {}", globalProperties.getProperty(GLOBAL_PROPERTY_TRUSTSTORE_TYPE));

        //TODO do we add a default value here? In case props are not present?
        HttpClientBuilder httpClientBuilder = HttpClientBuilder
                .create()
                .setSSLContext(getAlfrescoSolrSslContext(
                        globalProperties.getProperty(GLOBAL_PROPERTY_KEYSTORE_TYPE),
                        globalProperties.getProperty(GLOBAL_PROPERTY_KEYSTORE_PASSWORD),
                        globalProperties.getProperty(GLOBAL_PROPERTY_KEYSTORE_FILE_LOCATION),
                        globalProperties.getProperty(GLOBAL_PROPERTY_TRUSTSTORE_TYPE),
                        globalProperties.getProperty(GLOBAL_PROPERTY_TRUSTSTORE_PASSWORD),
                        globalProperties.getProperty(GLOBAL_PROPERTY_TRUSTSTORE_FILE_LOCATION)
                ));

        //By Default, the hostname is checked as well
        if (Boolean.parseBoolean(globalProperties.getProperty(GLOBAL_PROPERTY_HEALTHPROCESSOR_VALIDATE_HOSTNAMES, "true"))) {
            log.warn("MTLS host validation disabled.");
            httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        }

        return httpClientBuilder.build();
    }

    //Also see:https://github.com/xenit-eu/docker-alfresco/blob/7b067793afca467d940aefb8d2b5de3e959847f3/tomcat-base/src/shared/main/java/eu/xenit/alfresco/tomcat/embedded/alfresco/tomcat/AlfrescoTomcatFactoryHelper.java#L75C1-L80C105
    private SSLContext getAlfrescoSolrSslContext(
            String keystoreType,
            String keystorePass,
            String keystoreFileLocation,
            String truststoreType,
            String truststorePass,
            String truststoreFileLocation
    ) {
        try {
            KeyStore keystore = KeyStore.getInstance(keystoreType);
            keystore.load(Files.newInputStream(ResourceUtils.getFile(keystoreFileLocation).toPath()),
                    keystorePass.toCharArray());
            KeyStore trustStore = KeyStore.getInstance(truststoreType);
            trustStore.load(Files.newInputStream(ResourceUtils.getFile(truststoreFileLocation).toPath()),
                    truststorePass.toCharArray());
            return SSLContexts.custom().loadKeyMaterial(keystore, keystorePass.toCharArray())
                    .loadTrustMaterial(trustStore, (((chain, authType) -> false))).build();
        } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException |
                 UnrecoverableKeyException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }
}
