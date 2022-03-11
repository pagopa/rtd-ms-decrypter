package it.gov.pagopa.rtd.ms.rtdmsdecrypter.event;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.EventGridEvent;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.BlobRestConnectorImpl;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.DecrypterImpl;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

@Configuration
@Getter
public class EventHandler {

  @Bean
  public Consumer<Message<List<EventGridEvent>>> blobStorageConsumer(DecrypterImpl decrypterImpl,
      BlobRestConnectorImpl blobRestConnectorImpl) {
    // Should be wrapped in try-catch in case of malformed blobs' decryption
    return message -> message.getPayload().stream()
        .filter(e -> "Microsoft.Storage.BlobCreated".equals(e.getEventType()))
        .map(EventGridEvent::getSubject)
        .map(BlobApplicationAware::new)
        .filter(b -> !BlobApplicationAware.Application.NOAPP.equals(b.getApp()))
        .map(blobRestConnectorImpl::get)
        .map(decrypterImpl::decrypt)
        .map(blobRestConnectorImpl::put)
        .collect(Collectors.toList());
  }

}