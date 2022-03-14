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
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.DecrypterImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.apache.kafka.common.protocol.types.Field.Str;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;


@SpringBootTest
@EmbeddedKafka(topics = {
    "rtd-platform-events"}, partitions = 1, bootstrapServersProperty = "spring.cloud.stream.kafka.binder.brokers")
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class EventHandlerTest {


  @Autowired
  Consumer<Message<List<EventGridEvent>>> my_consumer;

  @MockBean
  private BlobRestConnectorImpl blobRestConnectorImpl;

  @MockBean
  private DecrypterImpl decrypterImpl;

  private final String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
  private final String myID = "my_id";
  private final String myTopic = "my_topic";
  private final String myEventType = "Microsoft.Storage.BlobCreated";

  //The test parameters reproduce the following scenarios: blobUriShouldPassRegex, blobUriShouldPassAlphnumABI
  @ParameterizedTest
  @ValueSource(strings = {"CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp",
      "CSTAR.a9911.TRNLOG.20220228.203107.001.csv.pgp"})
  void blobUriShouldPassRegex(String blobName, CapturedOutput output) {
    EventGridEvent my_event = new EventGridEvent();
    String blobUri = "/blobServices/default/containers/" + container + "/blobs/" + blobName;
    my_event.setId(myID);
    my_event.setTopic(myTopic);
    my_event.setEventType(myEventType);
    my_event.setSubject(blobUri);
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);
    Message<List<EventGridEvent>> msg = MessageBuilder.withPayload(my_list).build();

    //This test reaches the end of the handler, so the blob to be mocked in every status
    BlobApplicationAware blobDownloaded = new BlobApplicationAware(blobUri);
    BlobApplicationAware blobDecrypted = new BlobApplicationAware(blobUri);
    BlobApplicationAware blobUploaded = new BlobApplicationAware(blobUri);
    blobDownloaded.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    blobDecrypted.setStatus(BlobApplicationAware.Status.DECRYPTED);
    blobUploaded.setStatus(BlobApplicationAware.Status.UPLOADED);
    doReturn(blobDownloaded).when(blobRestConnectorImpl).get(any(BlobApplicationAware.class));
    doReturn(blobDecrypted).when(decrypterImpl).decrypt(any(BlobApplicationAware.class));
    doReturn(blobUploaded).when(blobRestConnectorImpl).put(any(BlobApplicationAware.class));

    my_consumer.accept(msg);
    verify(blobRestConnectorImpl, times(1)).get(any());
    assertThat(output.getOut(), not(containsString("Wrong name format:")));
    assertThat(output.getOut(), not(containsString("Conflicting service in URI:")));
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
      "CSTAR.99910.TRNLOG.20220228..001.csv.pgp", "CSTAR.99910.TRNLOG.20220228.103107.1.csv.pgp", "CSTAR.99910.TRNLOG.20220228.103107..csv.pgp"})
  void blobUriShouldFailRegex(String blobName, CapturedOutput output) {

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId(myID);
    my_event.setTopic(myTopic);
    my_event.setEventType(myEventType);
    my_event.setSubject("/blobServices/default/containers/" + container + "/blobs/" + blobName);
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);
    Message<List<EventGridEvent>> msg = MessageBuilder.withPayload(my_list).build();

    my_consumer.accept(msg);
    verify(blobRestConnectorImpl, times(0)).get(any());
    assertThat(output.getOut(), not(containsString("Conflicting service in URI:")));
    assertThat(output.getOut(), containsString("Wrong name format:"));
  }


  @ParameterizedTest
  @CsvSource({
      "ade-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y, CSTAR.99910.TRNLOG.20220228.203107.001.csv.pgp",
      "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y, ADE.99910.TRNLOG.20220228.203107.001.csv.pgp"})
  void blobUriShouldFailConflictingService(String container, String blobName,
      CapturedOutput output) {

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId(myID);
    my_event.setTopic(myTopic);
    my_event.setEventType(myEventType);
    my_event.setSubject("/blobServices/default/containers/" + container + "/blobs/" + blobName);
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);
    Message<List<EventGridEvent>> msg = MessageBuilder.withPayload(my_list).build();

    my_consumer.accept(msg);
    verify(blobRestConnectorImpl, times(0)).get(any());
    assertThat(output.getOut(), not(containsString("Wrong name format:")));
    assertThat(output.getOut(), containsString("Conflicting service in URI:"));
  }


  @Test
  void blobUriShouldIgnoreBecauseNotInteresting(CapturedOutput output) {

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId("my_id");
    my_event.setTopic("my_topic");
    my_event.setEventType("Microsoft.Storage.BlobCreated");
    my_event.setSubject("/blobServices/default/containers/cstar-exports/blobs/hashedPans_1.zip");
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);
    Message<List<EventGridEvent>> msg = MessageBuilder.withPayload(my_list).build();
    my_consumer.accept(msg);
    verify(blobRestConnectorImpl, times(0)).get(any());
    assertThat(output.getOut(), containsString("Event not of interest:"));
  }

}
