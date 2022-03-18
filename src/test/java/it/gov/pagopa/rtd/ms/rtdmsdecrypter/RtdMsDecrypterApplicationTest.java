package it.gov.pagopa.rtd.ms.rtdmsdecrypter;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.event.EventHandler;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Status;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.EventGridEvent;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.BlobRestConnectorImpl;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.DecrypterImpl;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.stream.messaging.DirectWithAttributesChannel;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(topics = {
    "rtd-platform-events"}, partitions = 1, bootstrapServersProperty = "spring.cloud.stream.kafka.binder.brokers")
@ExtendWith(OutputCaptureExtension.class)
class RtdMsDecrypterApplicationTest {

  @MockBean
  DecrypterImpl decrypterImpl;

  @SpyBean
  EventHandler handler;

  @MockBean
  BlobRestConnectorImpl blobRestConnectorImpl;

  @MockBean
  BlobApplicationAware blobApplicationAware;

  @Autowired
  private DirectWithAttributesChannel channel;

  private final String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
  private final String blob = "CSTAR.99910.TRNLOG.20220316.103107.001.csv.pgp";
  private final String blobUri = "/blobServices/default/containers/" + container + "/blobs/" + blob;

  private final String myID = "my_id";
  private final String myTopic = "my_topic";
  private final String myEventType = "Microsoft.Storage.BlobCreated";

  @Test
  void myshouldConsumeMessageAndCallDecrypter() {

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId(myID);
    my_event.setTopic(myTopic);
    my_event.setEventType(myEventType);
    my_event.setSubject(blobUri);
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);

    //Mock every step of the blob handling
    BlobApplicationAware blobDownloaded = new BlobApplicationAware(blobUri);
    BlobApplicationAware blobDecrypted = new BlobApplicationAware(blobUri);
    BlobApplicationAware blobUploaded = new BlobApplicationAware(blobUri);
    BlobApplicationAware blobDeleted = new BlobApplicationAware(blobUri);

    blobDownloaded.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    blobDecrypted.setStatus(BlobApplicationAware.Status.DECRYPTED);
    blobUploaded.setStatus(BlobApplicationAware.Status.UPLOADED);
    blobDeleted.setStatus(BlobApplicationAware.Status.DELETED);

    doReturn(blobDownloaded).when(blobRestConnectorImpl).get(any(BlobApplicationAware.class));
    doReturn(blobDecrypted).when(decrypterImpl).decrypt(any(BlobApplicationAware.class));
    doReturn(blobApplicationAware).when(blobRestConnectorImpl).put(any(BlobApplicationAware.class));

    //Mock of the interested blob's methods
    doReturn(blobDeleted).when(blobApplicationAware).localCleanup();
    doReturn(Status.UPLOADED).when(blobApplicationAware).getStatus();

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {

      channel.send(MessageBuilder.withPayload(my_list).build());

      // decrypt() is an inner call. Check first
      verify(blobRestConnectorImpl, times(1)).get(any());
      verify(decrypterImpl, times(1)).decrypt(any());
      verify(blobRestConnectorImpl, times(1)).put(any());
      verify(blobApplicationAware, times(1)).localCleanup();
      verify(handler, times(1)).blobStorageConsumer(any(), any());

    });
  }

  @Test
  void shouldFilterMessageForWrongService(CapturedOutput output) {

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId(myID);
    my_event.setTopic(myTopic);
    my_event.setEventType(myEventType);
    my_event.setSubject("/blobServices/default/containers/" + container
        + "/blobs/STAR.99910.TRNLOG.20220228.103107.001.csv.pgp");
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {

      channel.send(MessageBuilder.withPayload(my_list).build());

      // decrypt() is an inner call. Check first
      verify(blobRestConnectorImpl, times(0)).get(any());
      verify(decrypterImpl, times(0)).decrypt(any());
      verify(blobRestConnectorImpl, times(0)).put(any());
      verify(blobApplicationAware, times(0)).localCleanup();
      verify(handler, times(1)).blobStorageConsumer(any(), any());
      assertThat(output.getOut(), containsString("Wrong name format:"));
    });
  }

  @Test
  void shouldFilterMessageForFailedgGet() {

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId(myID);
    my_event.setTopic(myTopic);
    my_event.setEventType(myEventType);
    my_event.setSubject(blobUri);
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);

    BlobApplicationAware blobReceived = new BlobApplicationAware(blobUri);
    blobReceived.setStatus(BlobApplicationAware.Status.RECEIVED);
    doReturn(blobReceived).when(blobRestConnectorImpl).get(any(BlobApplicationAware.class));

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {

      channel.send(MessageBuilder.withPayload(my_list).build());

      // decrypt() is an inner call. Check first
      verify(blobRestConnectorImpl, times(1)).get(any());
      verify(decrypterImpl, times(0)).decrypt(any());
      verify(blobRestConnectorImpl, times(0)).put(any());
      verify(blobApplicationAware, times(0)).localCleanup();
      verify(handler, times(1)).blobStorageConsumer(any(), any());
    });
  }

  @Test
  void shouldFilterMessageForFailedDecrypt() {

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId(myID);
    my_event.setTopic(myTopic);
    my_event.setEventType(myEventType);
    my_event.setSubject(blobUri);
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);

    BlobApplicationAware blobDownloaded = new BlobApplicationAware(blobUri);
    blobDownloaded.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    doReturn(blobDownloaded).when(blobRestConnectorImpl).get(any(BlobApplicationAware.class));
    doReturn(blobDownloaded).when(decrypterImpl).decrypt(any(BlobApplicationAware.class));

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {

      channel.send(MessageBuilder.withPayload(my_list).build());

      // decrypt() is an inner call. Check first
      verify(blobRestConnectorImpl, times(1)).get(any());
      verify(decrypterImpl, times(1)).decrypt(any());
      verify(blobRestConnectorImpl, times(0)).put(any());
      verify(blobApplicationAware, times(0)).localCleanup();
      verify(handler, times(1)).blobStorageConsumer(any(), any());
    });
  }

  @Test
  void shouldFilterMessageForFailedPut() {

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId(myID);
    my_event.setTopic(myTopic);
    my_event.setEventType(myEventType);
    my_event.setSubject(blobUri);
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);

    BlobApplicationAware blobDownloaded = new BlobApplicationAware(blobUri);
    BlobApplicationAware blobDecrypted = new BlobApplicationAware(blobUri);
    blobDownloaded.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    blobDecrypted.setStatus(BlobApplicationAware.Status.DECRYPTED);
    doReturn(blobDownloaded).when(blobRestConnectorImpl).get(any(BlobApplicationAware.class));
    doReturn(blobDecrypted).when(decrypterImpl).decrypt(any(BlobApplicationAware.class));
    doReturn(blobDecrypted).when(blobRestConnectorImpl).put(any(BlobApplicationAware.class));

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {

      channel.send(MessageBuilder.withPayload(my_list).build());

      // decrypt() is an inner call. Check first
      verify(blobRestConnectorImpl, times(1)).get(any());
      verify(decrypterImpl, times(1)).decrypt(any());
      verify(blobRestConnectorImpl, times(1)).put(any());
      verify(blobApplicationAware, times(0)).localCleanup();
      verify(handler, times(1)).blobStorageConsumer(any(), any());
    });
  }
}