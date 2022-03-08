package it.gov.pagopa.rtd.ms.rtdmsdecrypter.configuration;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

@Configuration
public class ThreadSafeHttpClient {

  @Bean
  CloseableHttpClient myHttpClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    SSLContextBuilder builder = new SSLContextBuilder();
    builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
    SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(builder.build());
    return HttpClients.custom().setSSLSocketFactory(sslSocketFactory).setConnectionManager(new PoolingHttpClientConnectionManager()).build();
  }

}
