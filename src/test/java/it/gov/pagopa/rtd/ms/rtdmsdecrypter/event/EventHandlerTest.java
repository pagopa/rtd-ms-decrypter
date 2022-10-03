package it.gov.pagopa.rtd.ms.rtdmsdecrypter.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.EventGridEvent;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.BlobRestConnectorImpl;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.BlobSplitterImpl;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.BlobVerifierImpl;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.DecrypterImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;


@SpringBootTest
@EmbeddedKafka(topics = {
    "rtd-platform-events"}, partitions = 1, bootstrapServersProperty = "spring.cloud.stream.kafka.binder.brokers")
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "decrypt.enableChunkUpload=true",
})
class EventHandlerTest {


  @Autowired
  Consumer<Message<List<EventGridEvent>>> myConsumer;


  @MockBean
  private BlobRestConnectorImpl blobRestConnectorImpl;

  @MockBean
  private DecrypterImpl decrypterImpl;

  @MockBean
  private BlobSplitterImpl blobSplitter;

  @MockBean
  private BlobVerifierImpl blobVerifierImpl;

  private final String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
  private final String myID = "my_id";
  private final String myTopic = "my_topic";
  private final String myEventType = "Microsoft.Storage.BlobCreated";

  EventGridEvent myEvent;
  Message<List<EventGridEvent>> msg;
  List<EventGridEvent> myList;

  @BeforeEach
  void setUp() {
    myEvent = new EventGridEvent();
    myEvent.setId(myID);
    myEvent.setTopic(myTopic);
    myEvent.setEventType(myEventType);
    myList = new ArrayList<EventGridEvent>();
    myList.add(myEvent);
    msg = MessageBuilder.withPayload(myList).build();
  }

  //The test parameters reproduce the following scenarios: blobUriShouldPassRegex, blobUriShouldPassAlphnumABI
  @ParameterizedTest
  @ValueSource(strings = {"CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp",
      "CSTAR.a9911.TRNLOG.20220228.203107.001.csv.pgp"})
  void blobUriShouldPassRegex(String blobName) {

    String blobUri = "/blobServices/default/containers/" + container + "/blobs/" + blobName;
    myEvent.setSubject(blobUri);

    //This test reaches the end of the handler, so the blob to be mocked in every status
    BlobApplicationAware blobDownloaded = new BlobApplicationAware(blobUri);
    BlobApplicationAware blobDecrypted = new BlobApplicationAware(blobUri);
    BlobApplicationAware blobSplit = new BlobApplicationAware(blobUri);
    BlobApplicationAware blobVerified = new BlobApplicationAware(blobUri);
    BlobApplicationAware blobUploaded = new BlobApplicationAware(blobUri);
    blobDownloaded.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    blobDecrypted.setStatus(BlobApplicationAware.Status.DECRYPTED);
    blobVerified.setStatus(BlobApplicationAware.Status.VERIFIED);
    blobVerified.setOrigianalFileChunksNumber(3);
    blobSplit.setStatus(BlobApplicationAware.Status.SPLIT);
    blobUploaded.setStatus(BlobApplicationAware.Status.UPLOADED);
    doReturn(blobDownloaded).when(blobRestConnectorImpl).get(any(BlobApplicationAware.class));
    doReturn(blobDecrypted).when(decrypterImpl).decrypt(any(BlobApplicationAware.class));
    //Mock this method call by returning a stream of 3 copies of the same mocked blob
    doReturn(Stream.of(blobSplit, blobSplit, blobSplit)).when(blobSplitter)
        .split(any(BlobApplicationAware.class));
    doReturn(blobVerified).when(blobVerifierImpl).verify(any(BlobApplicationAware.class));
    doReturn(blobUploaded).when(blobRestConnectorImpl).put(any(BlobApplicationAware.class));

    myConsumer.accept(msg);
    verify(blobRestConnectorImpl, times(1)).get(any());
    verify(decrypterImpl, times(1)).decrypt(any());
    verify(blobSplitter, times(1)).split(any());
    verify(blobVerifierImpl, times(3)).verify(any());
    verify(blobRestConnectorImpl, times(3)).put(any());
  }

  //The test parameters reproduce the following scenarios: blobUriShouldFailWrongService, blobUriShouldFailNoService,
  // blobUriShouldFailShortABI, blobUriShouldFailLongABI, blobUriShouldFailNoABI, blobUriShouldFailWrongFiletype, blobUriShouldFailNoFiletype,
  // blobUriShouldFailWrongDate, blobUriShouldFailNoDate, blobUriShouldFailWrongTime, blobUriShouldFailNoTime, blobUriShouldFailWrongProgressive,
  // blobUriShouldFailNoProgressive
  @ParameterizedTest
  @ValueSource(strings = {"CSTA.99910.TRNLOG.20220228.203107.001.csv.pgp",
      ".99910.TRNLOG.20220228.203107.001.csv.pgp", "CSTAR.9991.TRNLOG.20220228.203107.001.csv.pgp",
      "CSTAR.999100.TRNLOG.20220228.203107.999.csv.pgp",
      "CSTAR..TRNLOG.20220228.203107.001.csv.pgp", "CSTAR.99910.TRNLO.20220228.203107.001.csv.pgp",
      "CSTAR.99910..20220228.203107.001.csv.pgp", "CSTAR.99910.TRNLOG.20220230.103107.001.csv.pgp",
      "CSTAR.99910.TRNLOG..103107.001.csv.pgp", "CSTAR.99910.TRNLOG.20220228.243107.001.csv.pgp",
      "CSTAR.99910.TRNLOG.20220228..001.csv.pgp", "CSTAR.99910.TRNLOG.20220228.103107.1.csv.pgp",
      "CSTAR.99910.TRNLOG.20220228.103107..csv.pgp"})
  void blobUriShouldFailRegex(String blobName) {

    String blobUri = "/blobServices/default/containers/" + container + "/blobs/" + blobName;
    myEvent.setSubject(blobUri);

    myConsumer.accept(msg);

    verify(blobRestConnectorImpl, times(0)).get(any());
    verify(decrypterImpl, times(0)).decrypt(any());
    verify(blobSplitter, times(0)).split(any());
    verify(blobVerifierImpl, times(0)).verify(any());
    verify(blobRestConnectorImpl, times(0)).put(any());
  }


  @ParameterizedTest
  @CsvSource({
      "ade-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y, CSTAR.99910.TRNLOG.20220228.203107.001.csv.pgp",
      "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y, ADE.99910.TRNLOG.20220228.203107.001.csv.pgp"})
  void blobUriShouldFailConflictingService(String container, String blobName) {

    String blobUri = "/blobServices/default/containers/" + container + "/blobs/" + blobName;
    myEvent.setSubject(blobUri);

    myConsumer.accept(msg);

    verify(blobRestConnectorImpl, times(0)).get(any());
    verify(decrypterImpl, times(0)).decrypt(any());
    verify(blobSplitter, times(0)).split(any());
    verify(blobVerifierImpl, times(0)).verify(any());
    verify(blobRestConnectorImpl, times(0)).put(any());
  }


  @Test
  void blobUriShouldIgnoreBecauseNotInteresting() {

    myEvent.setSubject("/blobServices/default/containers/cstar-exports/blobs/hashedPans_1.zip");

    myConsumer.accept(msg);

    verify(blobRestConnectorImpl, times(0)).get(any());
    verify(decrypterImpl, times(0)).decrypt(any());
    verify(blobSplitter, times(0)).split(any());
    verify(blobVerifierImpl, times(0)).verify(any());
    verify(blobRestConnectorImpl, times(0)).put(any());
  }

}
