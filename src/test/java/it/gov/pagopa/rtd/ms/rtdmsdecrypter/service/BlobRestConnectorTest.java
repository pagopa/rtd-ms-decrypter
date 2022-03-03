package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;


@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(topics = {
  "rtd-platform-events" }, partitions = 1, bootstrapServersProperty = "spring.cloud.stream.kafka.binder.brokers")
public class BlobRestConnectorTest {

  @Autowired
  BlobRestConnector blobRestConnector;

  @MockBean
  CloseableHttpClient client;
 
  @Test
  void shouldGet() throws ClientProtocolException, IOException {

    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blobName = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp";
    BlobApplicationAware blob = new BlobApplicationAware(
        "/blobServices/default/containers/" + container + "/blobs/" + blobName);

    blobRestConnector.get(blob);

    verify(client, times(1)).execute(any(HttpUriRequest.class), ArgumentMatchers.<ResponseHandler<OutputStream>>any());

  }
  
  @Test
  void shouldPut() throws ClientProtocolException, IOException {

    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blobName = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp";
    BlobApplicationAware blob = new BlobApplicationAware("/blobServices/default/containers/" + container + "/blobs/" + blobName);

    blobRestConnector.put(blob);

    verify(client, times(1)).execute(any(HttpPut.class));
   
  }
  
}
