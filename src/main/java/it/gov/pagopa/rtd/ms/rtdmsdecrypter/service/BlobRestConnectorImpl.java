package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Application;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.entity.EntityBuilder;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.FileEntity;
import org.apache.hc.core5.http.message.BasicHeader;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;

/**
 * Concrete implementation of a BlobRestConnector interface.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BlobRestConnectorImpl implements BlobRestConnector {

  @Value("${decrypt.api.baseurl}")
  private String baseUrl;

  @Value("${decrypt.blobclient.apikey}")
  private String blobApiKey;

  @Value("${decrypt.blobclient.basepath}")
  private String blobBasePath;

  public static final String BLOB_METADATA_PREFIX = "x-ms-meta-";

  public static final String SUB_KEY_HEADER = "Ocp-Apim-Subscription-Key";

  public static final String BLOB_METADATA_QUERY = "?comp=metadata";

  @Value("${decrypt.blobclient.sftp-basepath}")
  private String sftpBlobBasePath;

  private final HttpClient httpClient;

  /**
   * Method that allows the get of the blob from a remote storage.
   *
   * @param blob a blob that has been received from the event hub but not
   *             downloaded.
   * @return a locally available blob
   */
  public BlobApplicationAware get(BlobApplicationAware blob) {
    log.info("Start GET blob {} from {}", blob.getBlob(), blob.getContainer());
    String targetBlob = blob.getBlob();
    String targetBasePath = blobBasePath;

    if (blob.getApp().equals(Application.WALLET)) {
      targetBlob = "in/" + blob.getBlob();
      targetBasePath = sftpBlobBasePath;
    }

    String uri = baseUrl + "/" + targetBasePath + "/" + blob.getContainer() + "/" + targetBlob;

    final HttpGet getBlob = new HttpGet(uri);
    getBlob.setHeader(new BasicHeader(SUB_KEY_HEADER, blobApiKey));

    try {
      httpClient.execute(getBlob, downloadFileIn(blob));
      blob.setStatus(BlobApplicationAware.Status.DOWNLOADED);
      log.info("Successful GET of blob {} from {}", blob.getBlob(), blob.getContainer());
    } catch (ResponseStatusException ex) {
      log.error("Cannot GET blob {} from {}. Invalid HTTP response: {}, {}", blob.getBlob(),
          blob.getTargetContainer(), ex.getStatusCode().value(), ex.getReason());
    } catch (Exception ex) {
      log.error("Cannot GET blob {} from {}: {}", blob.getBlob(), blob.getContainer(), ex.getMessage());
    }

    return blob;
  }

  @NotNull
  protected HttpClientResponseHandler<Integer> downloadFileIn(BlobApplicationAware blob) {
    return response -> {
      if (response.getCode() != HttpStatus.SC_OK) {
        throw new ResponseStatusException(HttpStatusCode.valueOf(response.getCode()),
            response.getReasonPhrase());
      }
      return StreamUtils.copy(Objects.requireNonNull(response.getEntity().getContent()),
          new FileOutputStream(Path.of(blob.getTargetDir(), blob.getBlob()).toFile()));
    };
  }

  /**
   * Uploads a blob to remote storage.
   *
   * @param blob a blob locally available
   * @return an uploaded blob
   */
  public BlobApplicationAware put(BlobApplicationAware blob) {
    log.info("Start PUT blob {} to {}", blob.getBlob(), blob.getTargetContainer());

    String uri = baseUrl + "/" + blobBasePath + "/" + blob.getTargetContainer() + "/" + blob.getBlob();

    FileEntity entity = new FileEntity(
        Path.of(blob.getTargetDir(), blob.getBlob()).toFile(),
        ContentType.create("application/octet-stream"));

    final HttpPut putBlob = new HttpPut(uri);
    putBlob.setHeader(new BasicHeader(SUB_KEY_HEADER, blobApiKey));
    putBlob.setHeader(new BasicHeader("x-ms-blob-type", "BlockBlob"));
    putBlob.setHeader(new BasicHeader("x-ms-version", "2021-04-10"));
    putBlob.setHeader(new BasicHeader("If-None-Match", "*"));
    putBlob.setHeader(new BasicHeader(BLOB_METADATA_PREFIX + "numChunk", blob.getNumChunk()));
    putBlob.setHeader(new BasicHeader(BLOB_METADATA_PREFIX + "totalChunk", blob.getTotChunk()));
    putBlob.setEntity(entity);

    try {
      httpClient.execute(putBlob, validateStatusCode());
      blob.setStatus(BlobApplicationAware.Status.UPLOADED);
      log.info("Successful PUT of blob {} in {}", blob.getBlob(), blob.getTargetContainer());
    } catch (ResponseStatusException ex) {
      log.error("Cannot PUT blob {} in {}. Invalid HTTP response: {}, {}", blob.getBlob(),
          blob.getTargetContainer(), ex.getStatusCode().value(), ex.getReason());
    } catch (Exception ex) {
      log.error("Cannot PUT blob {} in {}. Unexpected error: {}", blob.getBlob(),
          blob.getTargetContainer(), ex.getMessage());
    }
    return blob;
  }

  @NotNull
  protected HttpClientResponseHandler<Void> validateStatusCode() {
    return response -> {
      int status = response.getCode();
      if (status != HttpStatus.SC_CREATED) {
        throw new ResponseStatusException(HttpStatusCode.valueOf(status),
            response.getReasonPhrase());
      }
      return null;
    };
  }

  @Override
  public BlobApplicationAware setMetadata(BlobApplicationAware blob) {
    // Temporary wallet skipping process
    if(blob.getApp()==Application.WALLET){
      return blob;
    }
    log.info("Start SET metadata for  {} to {}. CheckSum {}", blob.getBlob(), blob.getContainer(),blob.getReportMetaData().getCheckSum());

    String uri = baseUrl + "/" + blobBasePath + "/" + blob.getContainer() + "/" + blob.getBlob()
        + BLOB_METADATA_QUERY;

    final HttpPut setMetadata = new HttpPut(uri);
    setMetadata.setHeader(new BasicHeader(SUB_KEY_HEADER, blobApiKey));
    setMetadata.setHeader(
        new BasicHeader(BLOB_METADATA_PREFIX + "numMerchant", blob.getReportMetaData().getMerchantList().size()));
    setMetadata.setHeader(
        new BasicHeader(BLOB_METADATA_PREFIX + "numCanceledTrx", blob.getReportMetaData().getNumCanceledTrx()));
    setMetadata.setHeader(
        new BasicHeader(BLOB_METADATA_PREFIX + "numPositiveTrx", blob.getReportMetaData().getNumPositiveTrx()));
    setMetadata.setHeader(
        new BasicHeader(BLOB_METADATA_PREFIX + "totalAmountCanceledTrx",
            blob.getReportMetaData().getTotalAmountCanceledTrx()));
    setMetadata.setHeader(
        new BasicHeader(BLOB_METADATA_PREFIX + "totalAmountPositiveTrx",
            blob.getReportMetaData().getTotalAmountPositiveTrx()));
    setMetadata
        .setHeader(new BasicHeader(BLOB_METADATA_PREFIX + "maxAccountingDate",
            blob.getReportMetaData().getMaxAccountingDate().toString()));
    setMetadata
        .setHeader(new BasicHeader(BLOB_METADATA_PREFIX + "minAccountingDate",
            blob.getReportMetaData().getMinAccountingDate().toString()));
    setMetadata.setHeader(new BasicHeader(BLOB_METADATA_PREFIX + "checkSum", blob.getReportMetaData().getCheckSum()));
    
    setMetadata.setEntity(EntityBuilder.create().setText("")
        .setContentType(ContentType.create("text-plain", StandardCharsets.UTF_8)).build());

    try {
      httpClient.execute(setMetadata, validateStatusCodeSetMetadata());
      blob.setStatus(BlobApplicationAware.Status.ENRICHED);
      log.info("Successful SET metadata of blob {} in {}", blob.getBlob(), blob.getContainer());
    } catch (ResponseStatusException ex) {
      log.error("Cannot SET metadata for the blob {} in {}. Invalid HTTP response: {}, {}", blob.getBlob(),
          blob.getContainer(), ex.getStatusCode().value(), ex.getReason());
    } catch (IOException ex) {
      log.error("Cannot SET metadata for the blob {} in {}. Unexpected error: {}", blob.getBlob(),
          blob.getContainer(), ex.getMessage());
    }
    return blob;
  }

  @NotNull
  protected HttpClientResponseHandler<Void> validateStatusCodeSetMetadata() {
    return response -> {
      int status = response.getCode();
      if (status != HttpStatus.SC_OK) {
        throw new ResponseStatusException(HttpStatusCode.valueOf(status),
            response.getReasonPhrase());
      }
      return null;
    };
  }
}
