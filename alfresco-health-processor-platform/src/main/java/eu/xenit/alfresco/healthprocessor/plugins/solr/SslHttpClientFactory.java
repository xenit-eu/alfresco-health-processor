package eu.xenit.alfresco.healthprocessor.plugins.solr;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Objects;
import java.util.Properties;

public class SslHttpClientFactory {

    public static HttpClient setupHttpClient(Properties globalProperties) {
        try {
            String keystoreLocation = globalProperties.getProperty("encryption.ssl.keystore.location");
            String truststoreLocation = globalProperties.getProperty("encryption.ssl.truststore.location");

            Objects.requireNonNull(keystoreLocation);
            Objects.requireNonNull(truststoreLocation);

            File keystoreFile = new File(keystoreLocation);
            if(!keystoreFile.exists()) {
                throw new IndexOutOfBoundsException(
                    String.format("Keystore missing at location: %s",keystoreLocation)
                );
            }
            File keystoreParentLocation = keystoreFile.getParentFile();
            String keystorePassword = Objects.toString(getKeystorePassword(keystoreParentLocation));
            Objects.requireNonNull(keystorePassword);

            // Load the KeyStore
            KeyStore keyStore = KeyStore.getInstance("JCEKS");
            try (FileInputStream keystoreFileStream = new FileInputStream(keystoreLocation)) {
                keyStore.load(keystoreFileStream, keystorePassword.toCharArray());
            }

            // Load the TrustStore
            String truststorePassword = Objects.toString(getTruststorePassword(keystoreParentLocation));
            KeyStore trustStore = KeyStore.getInstance("JCEKS");
            try (FileInputStream truststoreFile = new FileInputStream(truststoreLocation)) {
                trustStore.load(truststoreFile, truststorePassword.toCharArray());
            }

            // Build SSLContext
            // If the key password is different from the keystore password,
            // retrieve it similarly if specified (else assume same)
            char[] keyPasswordChars = keystorePassword.toCharArray();

            javax.net.ssl.SSLContext sslContext = SSLContexts.custom()
                    .loadKeyMaterial(keyStore, keyPasswordChars)
                    .loadTrustMaterial(trustStore, (chain, authType) -> true) // Use default trust strategy
                    .build();

            // Create the SSLConnectionSocketFactory with the SSLContext
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                    sslContext,
                    new String[]{"TLSv1.2","TLSv1.3"}, // Allowed TLS protocols
                    null, // Default cipher suites
                    NoopHostnameVerifier.INSTANCE
            );
            return HttpClientBuilder.create()
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .setSSLSocketFactory(sslSocketFactory)
                    .build();
        } catch (IOException | CertificateException | KeyManagementException | UnrecoverableKeyException |
                 KeyStoreException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object getKeystorePassword(File keystoreParentLocation) throws IOException {
        return getProperty(
                keystoreParentLocation,
                "/ssl-keystore-passwords.properties",
                "keystore.password"
        );
    }

    private static Object getTruststorePassword(File keystoreParentLocation) throws IOException {
        return getProperty(
                keystoreParentLocation,
                "/ssl-truststore-passwords.properties",
                "keystore.password"
        );
    }

    private static Object getProperty(File propertiesFileParentLocation, String propertiesFileName,
                                        String propertyName) throws IOException {
        Objects.requireNonNull(propertiesFileParentLocation);
        Objects.requireNonNull(propertiesFileName);
        Objects.requireNonNull(propertyName);
        if(!propertiesFileParentLocation.exists()) {
            return null;
        }
        Path propertiesFile = Path.of(propertiesFileParentLocation.getAbsolutePath()
                                        + propertiesFileName);
        if(!propertiesFile.toFile().exists()) {
            return null;
        }
        try (InputStream stream = Files.newInputStream(propertiesFile)) {
            Properties p = new Properties();
            p.load(stream);
            return p.get(propertyName);
        }
    }
}
