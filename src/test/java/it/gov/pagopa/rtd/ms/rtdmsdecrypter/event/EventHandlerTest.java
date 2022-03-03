package it.gov.pagopa.rtd.ms.rtdmsdecrypter.event;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.EventGridEvent;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.BlobRestConnector;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.Decrypter;

@SpringBootTest
@EmbeddedKafka(topics = {
  "rtd-platform-events" }, partitions = 1, bootstrapServersProperty = "spring.cloud.stream.kafka.binder.brokers")
@ActiveProfiles("test")

public class EventHandlerTest {

  @Autowired
  Consumer<Message<List<EventGridEvent>>> my_consumer;

  @MockBean
  private BlobRestConnector blobRestConnector;
  
  @MockBean
  private Decrypter decrypter;

  @Test
  void blobUriShouldPassRegex() {
  
    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp";

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId("my_id");
    my_event.setTopic("my_topic");
    my_event.setEventType("Microsoft.Storage.BlobCreated");
    my_event.setSubject("/blobServices/default/containers/" + container + "/blobs/" + blob);
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);
    Message<List<EventGridEvent>> msg = MessageBuilder.withPayload(my_list).build();
    my_consumer.accept(msg);
    verify(blobRestConnector, times(1)).get(any());
  }
}
