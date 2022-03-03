package it.gov.pagopa.rtd.ms.rtdmsdecrypter.configuration;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThreadSafeHttpClient {

  @Bean
  CloseableHttpClient myHttpClient() {
    return HttpClients.custom().setConnectionManager(new PoolingHttpClientConnectionManager()).build();
  }
  
}
