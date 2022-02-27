package it.gov.pagopa.rtd.ms.rtdmsdecrypter;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.event.EventHandler;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.EventGridEvent;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.Decrypter;

import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.stream.messaging.DirectWithAttributesChannel;

import java.util.List;
import java.util.ArrayList;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

import java.time.Duration;


@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(topics = {"rtd-platform-events"},
		partitions = 1,
		bootstrapServersProperty = "spring.cloud.stream.kafka.binder.brokers")
public class RtdMsDecrypterApplicationTest {

  @SpyBean
  Decrypter decrypter;

  @SpyBean
  EventHandler handler;

  @Autowired
  private DirectWithAttributesChannel channel;

  @Test
  void shouldParseMessage() {

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId("myid");
    my_event.setTopic("xxxx");

    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {

			channel.send(MessageBuilder.withPayload(my_list).build());
      
			// decryot() is an inner call. Check first
			verify(decrypter, times(1)).decrypt(any());
			verify(handler, times(1)).blobStorageConsumer(any());

    });

  }
}