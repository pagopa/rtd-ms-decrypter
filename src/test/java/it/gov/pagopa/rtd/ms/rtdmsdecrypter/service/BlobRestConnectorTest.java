package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
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
 

  @Test
  void shouldGetSasToken() {
    // SasResponse myResponse = new SasResponse();
    // myResponse.setSas("sasToken");
    // myResponse.setAuthorizedContainer("container");
    // // when(client.postRtdSas(anyString(), anyString())).thenReturn(myResponse);
    // when(client.postAdeSas(anyString(), anyString())).thenReturn(myResponse);

    // blobRestConnector.postSas(IngestionApplication.RTD);

    // verify(client, times(1)).postRtdSas(anyString(), anyString());

    // blobRestConnector.postSas(IngestionApplication.ADE);

    // verify(client, times(1)).postAdeSas(anyString(), anyString());
  }

  @Test
  void shouldGet() {
    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blobName = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp";
    BlobApplicationAware blob = new BlobApplicationAware("/blobServices/default/containers/" + container + "/blobs/" + blobName);

    // blobRestConnector.get(blob);
   
  }
  
}
