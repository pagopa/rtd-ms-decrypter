package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
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


@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(topics = {
    "rtd-platform-events"}, partitions = 1, bootstrapServersProperty = "spring.cloud.stream.kafka.binder.brokers")
@ExtendWith(OutputCaptureExtension.class)
class BlobRestConnectorTest {

  @Autowired
  BlobRestConnectorImpl blobRestConnectorImpl;

  @MockBean
  CloseableHttpClient client;

  private final static String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
  private final static String blobName = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp";

  private final String EXCEPTION_MESSAGE = "Cannot connect.";

  private BlobApplicationAware blobIn;


  @BeforeEach
  public void setUp() {
    blobIn = new BlobApplicationAware(
        "/blobServices/default/containers/" + container + "/blobs/" + blobName);
  }

  @Test
  void shouldGet(CapturedOutput output) throws IOException {
    // Improvement idea: mock all the stuff needed in order to allow the FileDownloadResponseHandler
    // class to create a file in a temporary directory and test the content of the downloaded file
    // for an expected content.
    OutputStream mockedOutputStream = mock(OutputStream.class);
    doReturn(mockedOutputStream).when(client)
        .execute(any(HttpGet.class), any(BlobRestConnectorImpl.FileDownloadResponseHandler.class));

    BlobApplicationAware blobOut = blobRestConnectorImpl.get(blobIn);

    verify(client, times(1)).execute(any(HttpUriRequest.class),
        ArgumentMatchers.<ResponseHandler<OutputStream>>any());
    assertEquals(BlobApplicationAware.Status.DOWNLOADED, blobOut.getStatus());
    assertThat(output.getOut(), containsString("Successful GET of blob "));
    assertThat(output.getOut(), not(containsString("Cannot GET blob ")));

  }

  @Test
  void shouldFailGet(CapturedOutput output) throws IOException {
    doThrow(new IOException(EXCEPTION_MESSAGE)).when(client)

        .execute(any(HttpGet.class), any(BlobRestConnectorImpl.FileDownloadResponseHandler.class));

    BlobApplicationAware blobOut = blobRestConnectorImpl.get(blobIn);

    verify(client, times(1)).execute(any(HttpUriRequest.class),
        ArgumentMatchers.<ResponseHandler<OutputStream>>any());
    assertEquals(BlobApplicationAware.Status.RECEIVED, blobOut.getStatus());
    assertThat(output.getOut(), containsString("Cannot GET blob"));
    assertThat(output.getOut(), containsString("Cannot GET blob"));
  }

  @Test
  void shouldFailGetNullResponse(CapturedOutput output) throws IOException {

    BlobApplicationAware blobOut = blobRestConnectorImpl.get(blobIn);

    verify(client, times(1)).execute(any(HttpUriRequest.class),
        ArgumentMatchers.<ResponseHandler<OutputStream>>any());
    assertEquals(BlobApplicationAware.Status.RECEIVED, blobOut.getStatus());
    assertThat(output.getOut(), containsString("Cannot GET blob"));

  }

  @Test
  void shouldPut(CapturedOutput output) throws IOException {
    StatusLine mockedStatusLine = mock(StatusLine.class);
    doReturn(HttpStatus.SC_CREATED).when(mockedStatusLine).getStatusCode();
    CloseableHttpResponse mockedResponse = mock(CloseableHttpResponse.class);
    doReturn(mockedStatusLine).when(mockedResponse).getStatusLine();
    doReturn(mockedResponse).when(client).execute(any(HttpPut.class));

    BlobApplicationAware blobOut = blobRestConnectorImpl.put(blobIn);

    verify(client, times(1)).execute(any(HttpPut.class));
    assertEquals(BlobApplicationAware.Status.UPLOADED, blobOut.getStatus());
    assertThat(output.getOut(), not(containsString("Cannot PUT blob ")));
  }

  @Test
  void shouldFailPutHttpError(CapturedOutput output) throws IOException {
    StatusLine mockedStatusLine = mock(StatusLine.class);
    doReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR).when(mockedStatusLine).getStatusCode();
    doReturn("Internal Server Error").when(mockedStatusLine).getReasonPhrase();

    CloseableHttpResponse mockedResponse = mock(CloseableHttpResponse.class);
    doReturn(mockedStatusLine).when(mockedResponse).getStatusLine();
    doReturn(mockedResponse).when(client).execute(any(HttpPut.class));

    BlobApplicationAware blobOut = blobRestConnectorImpl.put(blobIn);

    verify(client, times(1)).execute(any(HttpPut.class));
    assertEquals(BlobApplicationAware.Status.RECEIVED, blobOut.getStatus());
    assertThat(output.getOut(), containsString("Cannot PUT blob "));
  }

  @Test
  void shouldFailPutUnexpectedError(CapturedOutput output) throws IOException {
    doThrow(new IOException(EXCEPTION_MESSAGE)).when(client)
        .execute(any(HttpGet.class), any(BlobRestConnectorImpl.FileDownloadResponseHandler.class));

    BlobApplicationAware blobOut = blobRestConnectorImpl.put(blobIn);

    verify(client, times(1)).execute(any(HttpPut.class));
    assertEquals(BlobApplicationAware.Status.RECEIVED, blobOut.getStatus());
    assertThat(output.getOut(), containsString("Cannot PUT blob "));

  }
}
