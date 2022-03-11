package it.gov.pagopa.rtd.ms.rtdmsdecrypter;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.event.EventHandler;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.EventGridEvent;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.BlobRestConnectorImpl;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.DecrypterImpl;

import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.stream.messaging.DirectWithAttributesChannel;

import java.util.List;
import java.util.ArrayList;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

import java.io.IOException;
import java.time.Duration;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(topics = {
    "rtd-platform-events" }, partitions = 1, bootstrapServersProperty = "spring.cloud.stream.kafka.binder.brokers")
class RtdMsDecrypterApplicationTestImpl {

  @MockBean
  DecrypterImpl decrypterImpl;

  @SpyBean
  EventHandler handler;

  @MockBean
  BlobRestConnectorImpl blobRestConnectorImpl;

  @Autowired
  private DirectWithAttributesChannel channel;

  @Test
  void shouldConsumeMessageAndCallDecrypter() throws IOException {

    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp";

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId("my_id");
    my_event.setTopic("my_topic");
    my_event.setEventType("Microsoft.Storage.BlobCreated");
    my_event.setSubject("/blobServices/default/containers/" + container + "/blobs/" + blob);
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {

      channel.send(MessageBuilder.withPayload(my_list).build());

      // decrypt() is an inner call. Check first
      verify(decrypterImpl, times(1)).decrypt(any());
      verify(handler, times(1)).blobStorageConsumer(any(), any());

    });

  }
}