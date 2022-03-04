package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
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

@Service
@Slf4j
public class BlobRestConnector implements IBlobRestConnector {


  @Value("${decrypt.api.baseurl}")
  private String baseUrl;

  @Value("${decrypt.blobclient.apikey}")
  private String blobApiKey;

  @Value("${decrypt.blobclient.basepath}")
  private String blobBasePath;

  @Autowired
  CloseableHttpClient httpClient;
  
  public BlobApplicationAware get(BlobApplicationAware blob) {

    String uri = baseUrl + "/" + blobBasePath + "/" + blob.getContainer() + "/" + blob.getBlob();
    final HttpGet getBlob = new HttpGet(uri);
    getBlob.setHeader(new BasicHeader("Ocp-Apim-Subscription-Key", blobApiKey));
    
    try {
      httpClient.execute(getBlob, new FileDownloadResponseHandler(new FileOutputStream(Path.of(blob.getTargetDir(), blob.getBlob()).toFile())));
    }
    catch (Exception ex) {
      log.error("GET Blob failed. {}", ex.getMessage());
    }

    blob.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    return blob;
  }

  public BlobApplicationAware put(BlobApplicationAware blob) {

    String uri = baseUrl + "/" + blobBasePath + "/" + blob.getTargetContainer() + "/" + blob.getBlob();

    FileEntity entity = new FileEntity(new File(Path.of(blob.getTargetDir(), blob.getBlob()).toString()),
        ContentType.create("application/octet-stream"));
    
    final HttpPut putBlob = new HttpPut(uri);
    putBlob.setHeader(new BasicHeader("Ocp-Apim-Subscription-Key", blobApiKey));
    putBlob.setEntity(entity);

    try {
      CloseableHttpResponse  myResponse = httpClient.execute(putBlob);
      assert (myResponse.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED);
    }
    catch (Exception ex) {
    }
    finally {
      blob.setStatus(BlobApplicationAware.Status.UPLOADED);
			IOUtils.closeQuietly(httpClient);
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
