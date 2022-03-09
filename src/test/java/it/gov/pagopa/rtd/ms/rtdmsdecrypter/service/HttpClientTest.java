package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.configuration.ThreadSafeHttpClient;

@SpringBootTest
@ContextConfiguration(classes = {ThreadSafeHttpClient.class})
@TestPropertySource(value = {"classpath:application-nokafka.yml"}, inheritProperties = false)
class HttpClientTest {

  @Autowired
  CloseableHttpClient myClient;

  @Test
  void shouldNotBeClosed() throws ClientProtocolException, IOException {
    String uri = "https://eu.httpbin.org/get";
    final HttpGet getBlob = new HttpGet(uri);
    CloseableHttpResponse response;
    response = myClient.execute(getBlob);
    assertEquals(200,response.getStatusLine().getStatusCode());
  }
  
  
}
