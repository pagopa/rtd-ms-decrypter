package it.gov.pagopa.rtd.ms.rtdmsdecrypter.event;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.Blob;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.EventGridEvent;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.BlobRestConnector;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.Decrypter;

@ActiveProfiles("test")
public class EventHandlerTest {

  private EventHandler eh;
  private BlobRestConnector blobRestConnector;
  private Decrypter decrypter;

  @Test
  void shouldMatchRegex() {
    eh = new EventHandler();
    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp";
    String blobUri = "/blobServices/default/containers/" + container + "/blobs/" + blob;
    assertTrue(eh.getEventPattern().matcher(blobUri).matches());
  }

  @Test
  void shouldNotMatchRegex() {
    eh = new EventHandler();
    String container = "rtd-loremipsum-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp";
    String blobUri = "/blobServices/default/containers/" + container + "/blobs/" + blob;
    assertFalse(eh.getEventPattern().matcher(blobUri).matches());
  }

  @Test
  void blobUriShouldPassRegex() {

    decrypter = spy(new Decrypter());
    blobRestConnector = spy(new BlobRestConnector());
    eh = new EventHandler();

    Consumer<Message<List<EventGridEvent>>> my_consumer = eh.blobStorageConsumer(decrypter, blobRestConnector);

    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp";

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId("my_id");
    my_event.setTopic("my_topic");
    my_event.setSubject("/blobServices/default/containers/" + container + "/blobs/" + blob);
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);
    Message<List<EventGridEvent>> msg = MessageBuilder.withPayload(my_list).build();
    my_consumer.accept(msg);
    //verify(blobRestConnector, times(1)).get(anyString());
  }
}
