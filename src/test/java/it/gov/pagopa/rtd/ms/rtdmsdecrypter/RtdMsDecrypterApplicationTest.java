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
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.BlobSplitterImpl;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.BlobVerifierImpl;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.DecrypterImpl;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.stream.messaging.DirectWithAttributesChannel;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(topics = {
    "rtd-platform-events"}, partitions = 1, bootstrapServersProperty = "spring.cloud.stream.kafka.binder.brokers")
@TestPropertySource(properties = {
    "decrypt.enableChunkUpload=true",
})
class RtdMsDecrypterApplicationTest {

  @Value("${decrypt.resources.base.path}")
  String resources;

  @MockBean
  DecrypterImpl decrypterImpl;

  @SpyBean
  EventHandler handler;

  @MockBean
  BlobRestConnectorImpl blobRestConnectorImpl;

  @MockBean
  BlobApplicationAware blobApplicationAware;

  @MockBean
  BlobSplitterImpl blobSplitterImpl;

  @MockBean
  BlobVerifierImpl blobVerifierImpl;

  @Autowired
  private DirectWithAttributesChannel channel;

  private final String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
  private final String blob = "CSTAR.99910.TRNLOG.20220316.103107.001.csv.pgp";
  private final String blobUri = "/blobServices/default/containers/" + container + "/blobs/" + blob;

  private final String myID = "my_id";
  private final String myTopic = "my_topic";
  private final String myEventType = "Microsoft.Storage.BlobCreated";
  private EventGridEvent myEvent;
  List<EventGridEvent> myList;

  @BeforeEach
  void setUp() {
    myEvent = new EventGridEvent();
    myList = new ArrayList<EventGridEvent>();
    myEvent.setId(myID);
    myEvent.setTopic(myTopic);
    myEvent.setEventType(myEventType);
    myEvent.setSubject(blobUri);
    myList.add(myEvent);
  }

  @Test
  void shouldConsumeMessage() {

    //Prepare fake blobs
    BlobApplicationAware blobDownloaded = new BlobApplicationAware(blobUri);
    BlobApplicationAware blobDecrypted = new BlobApplicationAware(blobUri);
    BlobApplicationAware blobSplit0 = new BlobApplicationAware(blobUri + ".0");
    BlobApplicationAware blobSplit1 = new BlobApplicationAware(blobUri + ".1");
    BlobApplicationAware blobSplit2 = new BlobApplicationAware(blobUri + ".2");
    BlobApplicationAware blobVerified = new BlobApplicationAware(blobUri);
    BlobApplicationAware blobUploaded = new BlobApplicationAware(blobUri);
    BlobApplicationAware blobDeleted = new BlobApplicationAware(blobUri);

    //Mock every step of the blob handling
    blobDownloaded.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    blobDecrypted.setStatus(BlobApplicationAware.Status.DECRYPTED);
    blobSplit0.setStatus(BlobApplicationAware.Status.SPLIT);
    blobSplit1.setStatus(BlobApplicationAware.Status.SPLIT);
    blobSplit2.setStatus(BlobApplicationAware.Status.SPLIT);
    blobVerified.setStatus(BlobApplicationAware.Status.VERIFIED);
    blobVerified.setOrigianalFileChunksNumber(3);
    blobUploaded.setStatus(BlobApplicationAware.Status.UPLOADED);
    blobDeleted.setStatus(BlobApplicationAware.Status.DELETED);

    //Mock the behaviour of the beans
    doReturn(blobDownloaded).when(blobRestConnectorImpl).get(any(BlobApplicationAware.class));
    doReturn(blobDecrypted).when(decrypterImpl).decrypt(any(BlobApplicationAware.class));
    doReturn(Stream.of(blobSplit0, blobSplit1, blobSplit2)).when(blobSplitterImpl)
        .split(any(BlobApplicationAware.class));
    doReturn(blobVerified).when(blobVerifierImpl).verify(any(BlobApplicationAware.class));
    doReturn(blobApplicationAware).when(blobRestConnectorImpl).put(any(BlobApplicationAware.class));

    //Mock of the interested blob's methods
    doReturn(blobDeleted).when(blobApplicationAware).localCleanup();
    doReturn(Status.UPLOADED).when(blobApplicationAware).getStatus();

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {

      //Send the message to the event grid
      channel.send(MessageBuilder.withPayload(myList).build());

      //Verify if every handling step is called the desired number of time
      verify(blobRestConnectorImpl, times(1)).get(any());
      verify(decrypterImpl, times(1)).decrypt(any());
      verify(blobSplitterImpl, times(1)).split(any());
      verify(blobVerifierImpl, times(3)).verify(any());
      verify(blobRestConnectorImpl, times(3)).put(any());
      verify(blobApplicationAware, times(3)).localCleanup();
      verify(handler, times(1)).blobStorageConsumer(any(), any(), any(), any());

    });
  }

  @Test
  void shouldFilterMessageForWrongService() {

    //Set wrong blob name
    myEvent.setSubject("/blobServices/default/containers/" + container
        + "/blobs/STAR.99910.TRNLOG.20220228.103107.001.csv.pgp");

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {

      //Send the message to the event grid
      channel.send(MessageBuilder.withPayload(myList).build());

      //Verify if every handling step is called the desired number of time
      verify(blobRestConnectorImpl, times(0)).get(any());
      verify(decrypterImpl, times(0)).decrypt(any());
      verify(blobSplitterImpl, times(0)).split(any());
      verify(blobVerifierImpl, times(0)).verify(any());
      verify(blobRestConnectorImpl, times(0)).put(any());
      verify(blobApplicationAware, times(0)).localCleanup();
      verify(handler, times(1)).blobStorageConsumer(any(), any(), any(), any());
    });
  }

  @Test
  void shouldFilterMessageForFailedgGet() {

    //Prepare fake blob
    BlobApplicationAware blobReceived = new BlobApplicationAware(blobUri);

    //Mock desired step of the blob handling
    //Keep the RECEIVED status, this will trigger the filter
    blobReceived.setStatus(BlobApplicationAware.Status.RECEIVED);

    //Mock the behaviour of the bean
    doReturn(blobReceived).when(blobRestConnectorImpl).get(any(BlobApplicationAware.class));

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {

      //Send the message to the event grid
      channel.send(MessageBuilder.withPayload(myList).build());

      //Verify if every handling step is called the desired number of time
      verify(blobRestConnectorImpl, times(1)).get(any());
      verify(decrypterImpl, times(0)).decrypt(any());
      verify(blobSplitterImpl, times(0)).split(any());
      verify(blobVerifierImpl, times(0)).verify(any());
      verify(blobRestConnectorImpl, times(0)).put(any());
      verify(blobApplicationAware, times(0)).localCleanup();
      verify(handler, times(1)).blobStorageConsumer(any(), any(), any(), any());
    });
  }

  @Test
  void shouldFilterMessageForFailedDecrypt() {

    //Prepare fake blob
    BlobApplicationAware blobDownloaded = new BlobApplicationAware(blobUri);
    BlobApplicationAware blobDecrypted = new BlobApplicationAware(blobUri);

    //Mock desired step of the blob handling
    blobDownloaded.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    //Keep the DOWNLOADED status, this will trigger the filter
    blobDecrypted.setStatus(BlobApplicationAware.Status.DOWNLOADED);

    //Mock the behaviour of the beans
    doReturn(blobDownloaded).when(blobRestConnectorImpl).get(any(BlobApplicationAware.class));
    doReturn(blobDecrypted).when(decrypterImpl).decrypt(any(BlobApplicationAware.class));

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {

      //Send the message to the event grid
      channel.send(MessageBuilder.withPayload(myList).build());

      //Verify if every handling step is called the desired number of time
      verify(blobRestConnectorImpl, times(1)).get(any());
      verify(decrypterImpl, times(1)).decrypt(any());
      verify(blobSplitterImpl, times(0)).split(any());
      verify(blobVerifierImpl, times(0)).verify(any());
      verify(blobRestConnectorImpl, times(0)).put(any());
      verify(blobApplicationAware, times(0)).localCleanup();
      verify(handler, times(1)).blobStorageConsumer(any(), any(), any(), any());

    });
  }

  @Test
  void shouldFilterMessageForFailedSplit() {

    //Prepare fake blobs
    BlobApplicationAware blobDownloaded = new BlobApplicationAware(blobUri);
    BlobApplicationAware blobDecrypted = new BlobApplicationAware(blobUri);
    BlobApplicationAware blobSplit = new BlobApplicationAware(blobUri);

    blobDownloaded.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    blobDecrypted.setStatus(BlobApplicationAware.Status.DECRYPTED);
    //Keep the DECRYPTED status, this will trigger the filter
    blobSplit.setStatus(BlobApplicationAware.Status.DECRYPTED);

    //Mock desired step of the blob handling
    doReturn(blobDownloaded).when(blobRestConnectorImpl).get(any(BlobApplicationAware.class));
    doReturn(blobDecrypted).when(decrypterImpl).decrypt(any(BlobApplicationAware.class));
    doReturn(Stream.of(blobSplit)).when(blobSplitterImpl).split(any(BlobApplicationAware.class));

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {

      //Send the message to the event grid
      channel.send(MessageBuilder.withPayload(myList).build());

      //Verify if every handling step is called the desired number of time
      verify(blobRestConnectorImpl, times(1)).get(any());
      verify(decrypterImpl, times(1)).decrypt(any());
      verify(blobSplitterImpl, times(1)).split(any());
      verify(blobVerifierImpl, times(0)).verify(any());
      verify(blobRestConnectorImpl, times(0)).put(any());
      verify(blobApplicationAware, times(0)).localCleanup();
      verify(handler, times(1)).blobStorageConsumer(any(), any(), any(), any());
    });
  }

  @Test
  void shouldFilterMessageForFailedVerify() {

    //Prepare fake blobs
    BlobApplicationAware blobDownloaded = new BlobApplicationAware(blobUri);
    BlobApplicationAware blobDecrypted = new BlobApplicationAware(blobUri);
    BlobApplicationAware blobSplit0 = new BlobApplicationAware(blobUri + ".0");
    BlobApplicationAware blobSplit1 = new BlobApplicationAware(blobUri + ".1");
    BlobApplicationAware blobSplit2 = new BlobApplicationAware(blobUri + ".2");
    BlobApplicationAware blobVerified = new BlobApplicationAware(blobUri);

    //Mock every step of the blob handling
    blobDownloaded.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    blobDecrypted.setStatus(BlobApplicationAware.Status.DECRYPTED);
    blobSplit0.setStatus(BlobApplicationAware.Status.SPLIT);
    blobSplit1.setStatus(BlobApplicationAware.Status.SPLIT);
    blobSplit2.setStatus(BlobApplicationAware.Status.SPLIT);
    blobVerified.setStatus(BlobApplicationAware.Status.VERIFIED);
    blobVerified.setOrigianalFileChunksNumber(3);

    //Mock the behaviour of the beans
    doReturn(blobDownloaded).when(blobRestConnectorImpl).get(any(BlobApplicationAware.class));
    doReturn(blobDecrypted).when(decrypterImpl).decrypt(any(BlobApplicationAware.class));
    doReturn(Stream.of(blobSplit0, blobSplit1, blobSplit2)).when(blobSplitterImpl)
        .split(any(BlobApplicationAware.class));
    doReturn(blobVerified).when(blobVerifierImpl).verify(blobSplit0);
    doReturn(blobVerified).when(blobVerifierImpl).verify(blobSplit2);
    //Fail verify second blob
    doReturn(blobSplit1).when(blobVerifierImpl).verify(any(BlobApplicationAware.class));

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {

      //Send the message to the event grid
      channel.send(MessageBuilder.withPayload(myList).build());

      //Verify if every handling step is called the desired number of time
      verify(blobRestConnectorImpl, times(1)).get(any());
      verify(decrypterImpl, times(1)).decrypt(any());
      verify(blobSplitterImpl, times(1)).split(any());
      verify(blobVerifierImpl, times(3)).verify(any());
      verify(blobRestConnectorImpl, times(0)).put(any());
      verify(blobApplicationAware, times(0)).localCleanup();
      verify(handler, times(1)).blobStorageConsumer(any(), any(), any(), any());
    });
  }

  @Test
  void shouldFilterMessageForFailedPut() {

    //Prepare fake blobs
    BlobApplicationAware blobDownloaded = new BlobApplicationAware(blobUri);
    BlobApplicationAware blobDecrypted = new BlobApplicationAware(blobUri);
    BlobApplicationAware blobSplit0 = new BlobApplicationAware(blobUri + ".0");
    BlobApplicationAware blobSplit1 = new BlobApplicationAware(blobUri + ".1");
    BlobApplicationAware blobSplit2 = new BlobApplicationAware(blobUri + ".2");
    BlobApplicationAware blobVerified = new BlobApplicationAware(blobUri);
    BlobApplicationAware blobUploaded = new BlobApplicationAware(blobUri);

    //Mock every step of the blob handling
    blobDownloaded.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    blobDecrypted.setStatus(BlobApplicationAware.Status.DECRYPTED);
    blobSplit0.setStatus(BlobApplicationAware.Status.SPLIT);
    blobSplit1.setStatus(BlobApplicationAware.Status.SPLIT);
    blobSplit2.setStatus(BlobApplicationAware.Status.SPLIT);
    blobVerified.setStatus(BlobApplicationAware.Status.VERIFIED);
    blobVerified.setOrigianalFileChunksNumber(3);
    //Keep the SPLIT status, this will trigger the filter
    blobUploaded.setStatus(BlobApplicationAware.Status.SPLIT);

    //Mock the behaviour of the beans
    doReturn(blobDownloaded).when(blobRestConnectorImpl).get(any(BlobApplicationAware.class));
    doReturn(blobDecrypted).when(decrypterImpl).decrypt(any(BlobApplicationAware.class));
    doReturn(Stream.of(blobSplit0, blobSplit1, blobSplit2)).when(blobSplitterImpl)
        .split(any(BlobApplicationAware.class));
    doReturn(blobVerified).when(blobVerifierImpl).verify(any(BlobApplicationAware.class));
    doReturn(blobUploaded).when(blobRestConnectorImpl).put(any(BlobApplicationAware.class));

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {

      //Send the message to the event grid
      channel.send(MessageBuilder.withPayload(myList).build());

      //Verify if every handling step is called the desired number of time
      verify(blobRestConnectorImpl, times(1)).get(any());
      verify(decrypterImpl, times(1)).decrypt(any());
      verify(blobSplitterImpl, times(1)).split(any());
      verify(blobVerifierImpl, times(3)).verify(any());
      verify(blobRestConnectorImpl, times(3)).put(any());
      verify(blobApplicationAware, times(0)).localCleanup();
      verify(handler, times(1)).blobStorageConsumer(any(), any(), any(), any());
    });
  }
}
