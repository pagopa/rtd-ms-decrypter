package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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


  public BlobApplicationAware get(BlobApplicationAware blob) {


    List<Header> headers = new ArrayList<>();
    headers.add(new BasicHeader("Ocp-Apim-Subscription-Key", blobApiKey));

    CloseableHttpClient httpclient = HttpClients.custom().setDefaultHeaders(headers).build();
    String uri = baseUrl + "/" + blobBasePath + "/" + blob.getContainer() + "/" + blob.getBlob();
    final HttpGet getBlob = new HttpGet(uri);

    try {
      httpclient.execute(getBlob, new FileDownloadResponseHandler(new FileOutputStream("/tmp/" + blob.getBlob())));
    }
    catch (Exception ex) {
    }
    finally {
      blob.setStatus(BlobApplicationAware.Status.DOWNLOADED);
			IOUtils.closeQuietly(httpclient);
		}

    return blob;
  }

  public BlobApplicationAware put(BlobApplicationAware blob) {

    List<Header> headers = new ArrayList<>();
    headers.add(new BasicHeader("Ocp-Apim-Subscription-Key", blobApiKey));

    CloseableHttpClient httpclient = HttpClients.custom().setDefaultHeaders(headers).build();
    String uri = baseUrl + "/" + blobBasePath + "/" + blob.getTargetContainer() + "/" + blob.getBlob();

    FileEntity entity = new FileEntity(new File("/tmp/" + blob.getBlob()),
        ContentType.create("application/octet-stream"));
    
    final HttpPut putBlob = new HttpPut(uri);
    putBlob.setEntity(entity);

    try {
      CloseableHttpResponse  myResponse= httpclient.execute(putBlob);
      assert (myResponse.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED);
    }
    catch (Exception ex) {
    }
    finally {
      blob.setStatus(BlobApplicationAware.Status.UPLOADED);
			IOUtils.closeQuietly(httpclient);
		}
    return blob;
  }


  static class FileDownloadResponseHandler implements ResponseHandler<OutputStream> {

		private final OutputStream target;

		public FileDownloadResponseHandler(OutputStream target) {
			this.target = target;
		}
		

    @Override
    public OutputStream handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
      StreamUtils.copy(Objects.requireNonNull(response.getEntity().getContent()), this.target);
			return this.target;
    }
		
	}

}
