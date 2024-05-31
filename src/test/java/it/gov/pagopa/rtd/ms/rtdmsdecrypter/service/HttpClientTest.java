package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ContextConfiguration(classes = {Mockito.class})
@TestPropertySource(value = {"classpath:application-nokafka.yml"}, inheritProperties = false)
class HttpClientTest {

  @Mock
  HttpClient client;

  @Test
  void shouldNotBeClosed() throws IOException {

    HttpResponse httpResponse = mock(HttpResponse.class);
    when(httpResponse.getCode()).thenReturn(200);
    when(client.execute(any(HttpGet.class))).thenReturn(httpResponse);
    String uri = "https://eu.httpbin.org/get";

    final HttpGet getBlob = new HttpGet(uri);
    HttpResponse res = client.execute(getBlob);

    assertEquals(200, res.getCode());
  }

}
