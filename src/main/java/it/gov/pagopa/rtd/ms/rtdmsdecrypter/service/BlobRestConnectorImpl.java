package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

/**
 * Concrete implementation of a BlobRestConnector interface.
 */
@Service
@Slf4j
public class BlobRestConnectorImpl implements BlobRestConnector {

  @Value("${decrypt.api.baseurl}")
  private String baseUrl;

  @Value("${decrypt.blobclient.apikey}")
  private String blobApiKey;

  @Value("${decrypt.blobclient.basepath}")
  private String blobBasePath;

  @Autowired
  CloseableHttpClient httpClient;

  /**
   * Constructor.
   *
   * @param blob a blob that has been received but not downloaded
   * @return a locally available blob
   */
  public BlobApplicationAware get(BlobApplicationAware blob) {

    String uri = baseUrl + "/" + blobBasePath + "/" + blob.getContainer() + "/" + blob.getBlob();
    final HttpGet getBlob = new HttpGet(uri);
    getBlob.setHeader(new BasicHeader("Ocp-Apim-Subscription-Key", blobApiKey));

    try {
      OutputStream result = httpClient.execute(getBlob,
          new FileDownloadResponseHandler(
              new FileOutputStream(Path.of(blob.getTargetDir(), blob.getBlob()).toFile())));
      result.close();
      blob.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    } catch (Exception ex) {
      log.error("GET Blob failed. {}", ex.getMessage());
    }

    return blob;
  }

  /**
   * Uploads a blob to remote storage.
   *
   * @param blob a blob locally available
   * @return an uploaded blob
   */
  public BlobApplicationAware put(BlobApplicationAware blob) {

    String uri =
        baseUrl + "/" + blobBasePath + "/" + blob.getTargetContainer() + "/" + blob.getBlob()
            + ".decrypted";

    FileEntity entity = new FileEntity(
        Path.of(blob.getTargetDir(), blob.getBlob() + ".decrypted").toFile(),
        ContentType.create("application/octet-stream"));

    final HttpPut putBlob = new HttpPut(uri);
    putBlob.setHeader(new BasicHeader("Ocp-Apim-Subscription-Key", blobApiKey));
    putBlob.setHeader(new BasicHeader("x-ms-blob-type", "BlockBlob"));
    putBlob.setHeader(new BasicHeader("x-ms-version", "2021-04-10"));
    putBlob.setEntity(entity);

    try {
      CloseableHttpResponse myResponse = httpClient.execute(putBlob);
      int status = myResponse.getStatusLine().getStatusCode();
      if (status == HttpStatus.SC_CREATED) {
        blob.setStatus(BlobApplicationAware.Status.UPLOADED);
      } else {
        log.error("Can't create blob {}. Invalid HTTP response: {}, {}", uri, status,
            myResponse.getStatusLine().getReasonPhrase());
      }
    } catch (Exception ex) {
      log.error("Can't create blob {}. Unexpected error: {}", uri, ex.getMessage());
    }
    return blob;
  }

  static class FileDownloadResponseHandler implements ResponseHandler<OutputStream> {

    private final OutputStream target;

    public FileDownloadResponseHandler(OutputStream target) {
      this.target = target;
    }

    @Override
    public OutputStream handleResponse(HttpResponse response) throws IOException {
      StreamUtils.copy(Objects.requireNonNull(response.getEntity().getContent()), this.target);
      return this.target;
    }

  }
}
