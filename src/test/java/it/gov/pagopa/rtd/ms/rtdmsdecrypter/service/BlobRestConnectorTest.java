package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;


@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(topics = {
  "rtd-platform-events" }, partitions = 1, bootstrapServersProperty = "spring.cloud.stream.kafka.binder.brokers")
@ExtendWith(OutputCaptureExtension.class)
class BlobRestConnectorTest {

  @Autowired
  BlobRestConnector blobRestConnector;

  @MockBean
  CloseableHttpClient client;

  private final static String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
  private final static String blobName = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp";

  private BlobApplicationAware blobIn;

  @BeforeEach
  public void setUp() {
    blobIn = new BlobApplicationAware("/blobServices/default/containers/" + container + "/blobs/" + blobName);
  }

  @Test
  void shouldGet(CapturedOutput output) throws IOException {
    // Improvement idea: mock all the stuff needed in order to allow the FileDownloadResponseHandler
    // class to create a file in a temporary directory and test the content of the downloaded file
    // for an expected content.
    OutputStream mockedOutputStream = mock(OutputStream.class);
    doReturn(mockedOutputStream).when(client).execute(any(HttpGet.class), any(BlobRestConnector.FileDownloadResponseHandler.class));

    BlobApplicationAware blobOut = blobRestConnector.get(blobIn);

    verify(client, times(1)).execute(any(HttpUriRequest.class), ArgumentMatchers.<ResponseHandler<OutputStream>>any());
    assertEquals(BlobApplicationAware.Status.DOWNLOADED, blobOut.getStatus());
    assertThat(output.getOut(), not(containsString("GET Blob failed")));
  }

  @Test
  void shouldPut(CapturedOutput output) throws IOException {
    StatusLine mockedStatusLine = mock(StatusLine.class);
    doReturn(HttpStatus.SC_CREATED).when(mockedStatusLine).getStatusCode();
    CloseableHttpResponse mockedResponse = mock(CloseableHttpResponse.class);
    doReturn(mockedStatusLine).when(mockedResponse).getStatusLine();
    doReturn(mockedResponse).when(client).execute(any(HttpPut.class));

    BlobApplicationAware blobOut = blobRestConnector.put(blobIn);

    verify(client, times(1)).execute(any(HttpPut.class));
    assertEquals(BlobApplicationAware.Status.UPLOADED, blobOut.getStatus());
    assertThat(output.getOut(), not(containsString("Can't create blob")));
  }

  @Test
  void shouldNotPut(CapturedOutput output) throws IOException {
    StatusLine mockedStatusLine = mock(StatusLine.class);
    doReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR).when(mockedStatusLine).getStatusCode();
    CloseableHttpResponse mockedResponse = mock(CloseableHttpResponse.class);
    doReturn(mockedStatusLine).when(mockedResponse).getStatusLine();
    doReturn(mockedResponse).when(client).execute(any(HttpPut.class));

    BlobApplicationAware blobOut = blobRestConnector.put(blobIn);

    verify(client, times(1)).execute(any(HttpPut.class));
    assertEquals(BlobApplicationAware.Status.RECEIVED, blobOut.getStatus());
    assertThat(output.getOut(), containsString("Invalid HTTP response: 500"));
  }
}
