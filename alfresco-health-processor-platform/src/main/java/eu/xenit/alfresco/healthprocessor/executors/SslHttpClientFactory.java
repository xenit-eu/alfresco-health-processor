package eu.xenit.alfresco.healthprocessor.executors;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.alfresco.encryption.AlfrescoKeyStore;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;

public class SslHttpClientFactory
{

    private static class TrustManagerDelegate implements X509TrustManager
    {

        private final X509TrustManager tm;

        private TrustManagerDelegate(X509TrustManager tm)
        {
            this.tm = tm;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
            tm.checkClientTrusted(chain, authType);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
            // NOOP
            // Use default trust strategy "Accept All", because:
            // 1. The certificate(s) is/are already in the docker image = should be trusted
            // 2. Requires extensive configurability to allow accepting certificates from various CA
            // (e.g. different customers have different CA)
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public X509Certificate[] getAcceptedIssuers()
        {
            return tm.getAcceptedIssuers();
        }
    }

    public static HttpClient setupHttpClient(AlfrescoKeyStore sslKeyStore, AlfrescoKeyStore sslTrustStore)
    {
        try
        {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManager[] trustManagers = sslTrustStore.createTrustManagers();
            for (int i = 0; i < trustManagers.length; i++)
            {
                if (trustManagers[i] instanceof X509TrustManager)
                {
                    trustManagers[i] = new TrustManagerDelegate((X509TrustManager) trustManagers[i]);
                }
            }
            sslContext.init(sslKeyStore.createKeyManagers(), trustManagers, null);

            // Create the SSLConnectionSocketFactory with the SSLContext
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, new String[] { "TLSv1.2", "TLSv1.3" },
                    null, NoopHostnameVerifier.INSTANCE);
            return HttpClientBuilder.create().setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).setSSLSocketFactory(sslSocketFactory)
                    .build();
        }
        catch (KeyManagementException | NoSuchAlgorithmException e)
        {
            throw new RuntimeException(e);
        }
    }
}
