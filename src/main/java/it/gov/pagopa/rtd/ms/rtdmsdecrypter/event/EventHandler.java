package it.gov.pagopa.rtd.ms.rtdmsdecrypter.event;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.EventGridEvent;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.BlobRestConnectorImpl;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.BlobSplitterImpl;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.BlobVerifierImpl;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.DecrypterImpl;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

/**
 * Component defining the processing steps in response to storage events.
 */
@Configuration
@Getter
@Slf4j
public class EventHandler {

  @Value("${decrypt.enableChunkUpload}")
  private boolean isChunkUploadEnabled;

  /**
   * Constructor.
   *
   * @param decrypterImpl         an instance of a Decrypter
   * @param blobRestConnectorImpl an instance of a BlobRestConnector
   * @param blobSplitterImpl      an instance of a BlobSplitter
   * @param blobVerifierImpl      an instance of a BlobVerifier
   * @return a consumer for Event Grid events
   */
  @Bean
  public Consumer<Message<List<EventGridEvent>>> blobStorageConsumer(DecrypterImpl decrypterImpl,
      BlobRestConnectorImpl blobRestConnectorImpl, BlobSplitterImpl blobSplitterImpl,
      BlobVerifierImpl blobVerifierImpl) {

    log.info("Chunks upload enabled: {}", isChunkUploadEnabled);

    return message -> {
      log.info("message body {}", message.getPayload().toString());
      log.info("message body size {}", message.getPayload().size());
      List<BlobApplicationAware> chunks = message.getPayload().stream()
          .map(e -> {
            log.info(e.toString());
            return e;
          })
          .filter(e -> "Microsoft.Storage.BlobCreated".equals(e.getEventType())
              || "Microsoft.Storage.BlobRenamed".equals(e.getEventType()))
          .map(EventGridEvent::getSubject)
          .map(BlobApplicationAware::new)
          .filter(b -> !BlobApplicationAware.Application.NOAPP.equals(b.getApp()))
          .map(blobRestConnectorImpl::get)
          .filter(b -> BlobApplicationAware.Status.DOWNLOADED.equals(b.getStatus()))
          .map(decrypterImpl::decrypt)
          .filter(b -> BlobApplicationAware.Status.DECRYPTED.equals(b.getStatus()))
          .flatMap(blobSplitterImpl::split)
          .filter(b -> BlobApplicationAware.Status.SPLIT.equals(b.getStatus()))
          .toList();

      List<BlobApplicationAware> verifiedChunks = chunks.stream()
          .map(blobVerifierImpl::verify)
          .filter(b -> BlobApplicationAware.Status.VERIFIED.equals(b.getStatus()))
          .toList();

      Optional<BlobApplicationAware> originalBlob = chunks.stream().findFirst()
          .map(BlobApplicationAware::getOriginalBlob);

      if (!chunks.isEmpty()) {
        if (verifiedChunks.size() == chunks.size()) {
          long uploadedChunks = verifiedChunks.stream()
              .map(b -> isChunkUploadEnabled ? blobRestConnectorImpl.put(b) : b)
              .filter(b -> BlobApplicationAware.Status.UPLOADED.equals(b.getStatus()))
              .count();
          log.info("Uploaded chunks: {}", uploadedChunks);
          blobRestConnectorImpl.setMetadata(originalBlob.get());
        } else {
          log.error("Not all chunks are verified, no chunks will be uploaded of {}",
              chunks.get(0).getOriginalBlobName());
        }

        long deletedChunks = chunks.stream()
            .map(BlobApplicationAware::localCleanup)
            .filter(b -> BlobApplicationAware.Status.DELETED.equals(b.getStatus())).count();

        log.info("Deleted {}/{} chunks of blob: {}", deletedChunks, chunks.size(),
            originalBlob.get().getBlob());

        log.info("Handled blob: {}", originalBlob.get().getBlob());
      }
    };
  }
}
