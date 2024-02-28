package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.configuration.ThreadSafeHttpClient;
import java.io.IOException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ContextConfiguration(classes = {ThreadSafeHttpClient.class})
@TestPropertySource(value = {"classpath:application-nokafka.yml"}, inheritProperties = false)
class HttpClientTest {

  @Autowired
  CloseableHttpClient myClient;

  @Test
  void shouldNotBeClosed() throws IOException {
    String uri = "https://eu.httpbin.org/get";
    final HttpGet getBlob = new HttpGet(uri);
    var statusCode = myClient.execute(getBlob, HttpResponse::getCode);

    assertEquals(200, statusCode);
  }

}
