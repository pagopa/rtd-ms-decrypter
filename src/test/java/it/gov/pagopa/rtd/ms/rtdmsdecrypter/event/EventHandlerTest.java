package it.gov.pagopa.rtd.ms.rtdmsdecrypter.event;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.EventGridEvent;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.BlobRestConnectorImpl;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.DecrypterImpl;

@SpringBootTest
@EmbeddedKafka(topics = {
  "rtd-platform-events" }, partitions = 1, bootstrapServersProperty = "spring.cloud.stream.kafka.binder.brokers")
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class EventHandlerTest {

  @Autowired
  Consumer<Message<List<EventGridEvent>>> my_consumer;

  @MockBean
  private BlobRestConnectorImpl blobRestConnectorImpl;
  
  @MockBean
  private DecrypterImpl decrypterImpl;

  @Test
  void blobUriShouldPassRegex(CapturedOutput output) {
  
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
    verify(blobRestConnectorImpl, times(1)).get(any());
    assertThat(output.getOut(), not(containsString("Wrong name format:")));
  }

  @Test
  void blobUriShouldFailWrongService(CapturedOutput output) {

    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "CSTA.99910.TRNLOG.20220228.203107.999.csv.pgp";

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId("my_id");
    my_event.setTopic("my_topic");
    my_event.setEventType("Microsoft.Storage.BlobCreated");
    my_event.setSubject("/blobServices/default/containers/" + container + "/blobs/" + blob);
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);
    Message<List<EventGridEvent>> msg = MessageBuilder.withPayload(my_list).build();
    my_consumer.accept(msg);
    verify(blobRestConnectorImpl, times(0)).get(any());
    assertThat(output.getOut(), containsString("Wrong name format:"));
  }

  @Test
  void blobUriShouldFailNoService(CapturedOutput output) {

    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "..99910.TRNLOG.20220228.203107.999.csv.pgp";

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId("my_id");
    my_event.setTopic("my_topic");
    my_event.setEventType("Microsoft.Storage.BlobCreated");
    my_event.setSubject("/blobServices/default/containers/" + container + "/blobs/" + blob);
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);
    Message<List<EventGridEvent>> msg = MessageBuilder.withPayload(my_list).build();
    my_consumer.accept(msg);
    verify(blobRestConnectorImpl, times(0)).get(any());
    assertThat(output.getOut(), containsString("Wrong name format:"));
  }

  @Test
  void blobUriShouldPassAlphnumABI(CapturedOutput output) {

    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "CSTAR.a9911.TRNLOG.20220228.203107.999.csv.pgp";

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId("my_id");
    my_event.setTopic("my_topic");
    my_event.setEventType("Microsoft.Storage.BlobCreated");
    my_event.setSubject("/blobServices/default/containers/" + container + "/blobs/" + blob);
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);
    Message<List<EventGridEvent>> msg = MessageBuilder.withPayload(my_list).build();
    my_consumer.accept(msg);
    verify(blobRestConnectorImpl, times(1)).get(any());
    assertThat(output.getOut(), not(containsString("Wrong name format:")));
  }

  @Test
  void blobUriShouldFailShortABI(CapturedOutput output) {

    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "CSTAR.9991.TRNLOG.20220228.203107.999.csv.pgp";

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId("my_id");
    my_event.setTopic("my_topic");
    my_event.setEventType("Microsoft.Storage.BlobCreated");
    my_event.setSubject("/blobServices/default/containers/" + container + "/blobs/" + blob);
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);
    Message<List<EventGridEvent>> msg = MessageBuilder.withPayload(my_list).build();
    my_consumer.accept(msg);
    verify(blobRestConnectorImpl, times(0)).get(any());
    assertThat(output.getOut(), containsString("Wrong name format:"));
  }

  @Test
  void blobUriShouldFailLongABI(CapturedOutput output) {

    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "CSTAR.999100.TRNLOG.20220228.203107.999.csv.pgp";

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId("my_id");
    my_event.setTopic("my_topic");
    my_event.setEventType("Microsoft.Storage.BlobCreated");
    my_event.setSubject("/blobServices/default/containers/" + container + "/blobs/" + blob);
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);
    Message<List<EventGridEvent>> msg = MessageBuilder.withPayload(my_list).build();
    my_consumer.accept(msg);
    verify(blobRestConnectorImpl, times(0)).get(any());
    assertThat(output.getOut(), containsString("Wrong name format:"));
  }

  @Test
  void blobUriShouldFailNoABI(CapturedOutput output) {

    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "CSTA..TRNLOG.20220228.203107.999.csv.pgp";

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId("my_id");
    my_event.setTopic("my_topic");
    my_event.setEventType("Microsoft.Storage.BlobCreated");
    my_event.setSubject("/blobServices/default/containers/" + container + "/blobs/" + blob);
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);
    Message<List<EventGridEvent>> msg = MessageBuilder.withPayload(my_list).build();
    my_consumer.accept(msg);
    verify(blobRestConnectorImpl, times(0)).get(any());
    assertThat(output.getOut(), containsString("Wrong name format:"));
  }

  @Test
  void blobUriShouldFailWrongFiletype(CapturedOutput output) {

    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "CSTA.99910.TRNLO.20220228.203107.999.csv.pgp";

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId("my_id");
    my_event.setTopic("my_topic");
    my_event.setEventType("Microsoft.Storage.BlobCreated");
    my_event.setSubject("/blobServices/default/containers/" + container + "/blobs/" + blob);
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);
    Message<List<EventGridEvent>> msg = MessageBuilder.withPayload(my_list).build();
    my_consumer.accept(msg);
    verify(blobRestConnectorImpl, times(0)).get(any());
    assertThat(output.getOut(), containsString("Wrong name format:"));
  }

  @Test
  void blobUriShouldFailNoFiletype(CapturedOutput output) {

    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "CSTA.99910..20220228.203107.999.csv.pgp";

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId("my_id");
    my_event.setTopic("my_topic");
    my_event.setEventType("Microsoft.Storage.BlobCreated");
    my_event.setSubject("/blobServices/default/containers/" + container + "/blobs/" + blob);
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);
    Message<List<EventGridEvent>> msg = MessageBuilder.withPayload(my_list).build();
    my_consumer.accept(msg);
    verify(blobRestConnectorImpl, times(0)).get(any());
    assertThat(output.getOut(), containsString("Wrong name format:"));
  }

  @Test
  void blobUriShouldFaileWrongDate(CapturedOutput output) {

    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "CSTAR.99910.TRNLOG.20220230.103107.001.csv.pgp";

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId("my_id");
    my_event.setTopic("my_topic");
    my_event.setEventType("Microsoft.Storage.BlobCreated");
    my_event.setSubject("/blobServices/default/containers/" + container + "/blobs/" + blob);
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);
    Message<List<EventGridEvent>> msg = MessageBuilder.withPayload(my_list).build();
    my_consumer.accept(msg);
    verify(blobRestConnectorImpl, times(0)).get(any());
    assertThat(output.getOut(), containsString("Wrong name format:"));
  }

  @Test
  void blobUriShouldFaileNoDate(CapturedOutput output) {

    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "CSTAR.99910.TRNLOG..103107.001.csv.pgp";

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId("my_id");
    my_event.setTopic("my_topic");
    my_event.setEventType("Microsoft.Storage.BlobCreated");
    my_event.setSubject("/blobServices/default/containers/" + container + "/blobs/" + blob);
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);
    Message<List<EventGridEvent>> msg = MessageBuilder.withPayload(my_list).build();
    my_consumer.accept(msg);
    verify(blobRestConnectorImpl, times(0)).get(any());
    assertThat(output.getOut(), containsString("Wrong name format:"));
  }

  @Test
  void blobUriShouldFaileWrongTime(CapturedOutput output) {

    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "CSTAR.99910.TRNLOG.20220228.243107.001.csv.pgp";

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId("my_id");
    my_event.setTopic("my_topic");
    my_event.setEventType("Microsoft.Storage.BlobCreated");
    my_event.setSubject("/blobServices/default/containers/" + container + "/blobs/" + blob);
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);
    Message<List<EventGridEvent>> msg = MessageBuilder.withPayload(my_list).build();
    my_consumer.accept(msg);
    verify(blobRestConnectorImpl, times(0)).get(any());
    assertThat(output.getOut(), containsString("Wrong name format:"));
  }

  @Test
  void blobUriShouldFaileNoTime(CapturedOutput output) {

    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "CSTAR.99910.TRNLOG.20220228..001.csv.pgp";

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId("my_id");
    my_event.setTopic("my_topic");
    my_event.setEventType("Microsoft.Storage.BlobCreated");
    my_event.setSubject("/blobServices/default/containers/" + container + "/blobs/" + blob);
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);
    Message<List<EventGridEvent>> msg = MessageBuilder.withPayload(my_list).build();
    my_consumer.accept(msg);
    verify(blobRestConnectorImpl, times(0)).get(any());
    assertThat(output.getOut(), containsString("Wrong name format:"));
  }

  @Test
  void blobUriShouldFailWrongProgressive(CapturedOutput output) {

    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "CSTAR.99910.TRNLOG.20220228.103107.1.csv.pgp";

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId("my_id");
    my_event.setTopic("my_topic");
    my_event.setEventType("Microsoft.Storage.BlobCreated");
    my_event.setSubject("/blobServices/default/containers/" + container + "/blobs/" + blob);
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);
    Message<List<EventGridEvent>> msg = MessageBuilder.withPayload(my_list).build();
    my_consumer.accept(msg);
    verify(blobRestConnectorImpl, times(0)).get(any());
    assertThat(output.getOut(), containsString("Wrong name format:"));
  }

  @Test
  void blobUriShouldFailNoProgressive(CapturedOutput output) {

    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "CSTAR.99910.TRNLOG.20220228.103107..csv.pgp";

    EventGridEvent my_event = new EventGridEvent();
    my_event.setId("my_id");
    my_event.setTopic("my_topic");
    my_event.setEventType("Microsoft.Storage.BlobCreated");
    my_event.setSubject("/blobServices/default/containers/" + container + "/blobs/" + blob);
    List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
    my_list.add(my_event);
    Message<List<EventGridEvent>> msg = MessageBuilder.withPayload(my_list).build();
    my_consumer.accept(msg);
    verify(blobRestConnectorImpl, times(0)).get(any());
    assertThat(output.getOut(), containsString("Wrong name format:"));
  }

}
