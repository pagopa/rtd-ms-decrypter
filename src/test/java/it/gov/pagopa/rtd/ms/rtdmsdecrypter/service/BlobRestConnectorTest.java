package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Application;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.impl.io.DefaultClassicHttpResponseFactory;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.BasicHttpEntity;
import org.apache.kafka.storage.internals.log.AppendOrigin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith({ OutputCaptureExtension.class, MockitoExtension.class })
class BlobRestConnectorTest {

  BlobRestConnectorImpl blobRestConnectorImpl;

  @Mock
  CloseableHttpClient client;

  private final static String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";

  private final static String containerWallet = "nexi";

  private final static String directoryWallet = "in";

  private final static String blobName = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp";

  private final static String blobNameWallet = "PAGOPAPM_NPG_CONTRACTS_20240322000000_001_OUT";

  private final String EXCEPTION_MESSAGE = "Cannot connect.";

  private BlobApplicationAware blobIn;

  private BlobApplicationAware blobInWallet;

  @BeforeEach
  public void setUp() {
    blobIn = new BlobApplicationAware(
        "/blobServices/default/containers/" + container + "/blobs/" + blobName);
    blobInWallet = new BlobApplicationAware(
        "/blobServices/default/containers/" + containerWallet + "/blobs/" + directoryWallet + "/"
            + blobNameWallet);
    blobRestConnectorImpl = new BlobRestConnectorImpl(client);
  }

  @Test
  void shouldGet(CapturedOutput output) throws IOException {
    BlobApplicationAware blobOut = blobRestConnectorImpl.get(blobIn);

    verify(client, times(1)).execute(any(HttpUriRequest.class),
        any(HttpClientResponseHandler.class));
    assertEquals(BlobApplicationAware.Status.DOWNLOADED, blobOut.getStatus());
    assertThat(output.getOut(), containsString("Successful GET of blob "));
    assertThat(output.getOut(), not(containsString("Cannot GET blob ")));
  }

  @Test
  void shouldGetWalletFile(CapturedOutput output) throws IOException {
    BlobApplicationAware blobOut = blobRestConnectorImpl.get(blobInWallet);

    verify(client, times(1)).execute(any(HttpUriRequest.class),
        any(HttpClientResponseHandler.class));
    assertEquals(BlobApplicationAware.Status.DOWNLOADED, blobOut.getStatus());
    assertThat(output.getOut(), containsString("Successful GET of blob "));
    assertThat(output.getOut(), not(containsString("Cannot GET blob ")));
  }

  @Test
  void shouldFailGet(CapturedOutput output) throws IOException {
    doThrow(new IOException(EXCEPTION_MESSAGE)).when(client)
        .execute(any(HttpGet.class), any(HttpClientResponseHandler.class));

    BlobApplicationAware blobOut = blobRestConnectorImpl.get(blobIn);

    verify(client, times(1)).execute(any(HttpUriRequest.class),
        any(HttpClientResponseHandler.class));
    assertEquals(BlobApplicationAware.Status.RECEIVED, blobOut.getStatus());
    assertThat(output.getOut(), containsString("Cannot GET blob"));
  }

  @Test
  void shouldFailGetHttpError(CapturedOutput output) throws IOException {
    doThrow(new ResponseStatusException(HttpStatusCode.valueOf(404), "not_found"))
        .when(client).execute(any(HttpGet.class), any(HttpClientResponseHandler.class));

    BlobApplicationAware blobOut = blobRestConnectorImpl.get(blobIn);

    verify(client, times(1)).execute(any(HttpGet.class), any(HttpClientResponseHandler.class));
    assertEquals(BlobApplicationAware.Status.RECEIVED, blobOut.getStatus());
    assertThat(output.getOut(), containsString("Cannot GET blob "));
  }

  @Test
  void shouldFailGetNullResponse(CapturedOutput output) throws IOException {
    doThrow(new NullPointerException()).when(client)
        .execute(any(HttpGet.class), any(HttpClientResponseHandler.class));

    BlobApplicationAware blobOut = blobRestConnectorImpl.get(blobIn);

    verify(client, times(1)).execute(any(HttpUriRequest.class),
        any(HttpClientResponseHandler.class));
    assertEquals(BlobApplicationAware.Status.RECEIVED, blobOut.getStatus());
    assertThat(output.getOut(), containsString("Cannot GET blob"));
  }

  @Test
  void shouldPut(CapturedOutput output) throws IOException {
    BlobApplicationAware blobOut = blobRestConnectorImpl.put(blobIn);

    verify(client, times(1)).execute(any(HttpPut.class), any(HttpClientResponseHandler.class));
    assertEquals(BlobApplicationAware.Status.UPLOADED, blobOut.getStatus());
    assertThat(output.getOut(), not(containsString("Cannot PUT blob ")));
  }

  @Test
  void shouldFailPutHttpError(CapturedOutput output) throws IOException {
    doThrow(new ResponseStatusException(HttpStatusCode.valueOf(404), "not_found"))
        .when(client).execute(any(HttpPut.class), any(HttpClientResponseHandler.class));

    BlobApplicationAware blobOut = blobRestConnectorImpl.put(blobIn);

    verify(client, times(1)).execute(any(HttpPut.class), any(HttpClientResponseHandler.class));
    assertEquals(BlobApplicationAware.Status.RECEIVED, blobOut.getStatus());
    assertThat(output.getOut(), containsString("Cannot PUT blob "));
  }

  @Test
  void shouldFailPutUnexpectedError(CapturedOutput output) throws IOException {
    doThrow(new IOException(EXCEPTION_MESSAGE)).when(client)
        .execute(any(HttpPut.class), any(HttpClientResponseHandler.class));

    BlobApplicationAware blobOut = blobRestConnectorImpl.put(blobIn);

    verify(client, times(1)).execute(any(HttpPut.class), any(HttpClientResponseHandler.class));
    assertEquals(BlobApplicationAware.Status.RECEIVED, blobOut.getStatus());
    assertThat(output.getOut(), containsString("Cannot PUT blob "));
  }

  @Test
  void givenGetResponse200ThenReturnsInt() throws HttpException, IOException {
    var response = DefaultClassicHttpResponseFactory.INSTANCE.newHttpResponse(200, "test");
    response.setEntity(
        new BasicHttpEntity(IOUtils.toInputStream("fake_content", StandardCharsets.UTF_8),
            ContentType.TEXT_PLAIN));

    var byteCopied = blobRestConnectorImpl.downloadFileIn(blobIn).handleResponse(response);

    assertThat(byteCopied, greaterThan(0));
  }

  @ParameterizedTest
  @ValueSource(ints = { 201, 202, 203, 301, 400, 404, 500, 502 })
  void givenBadStatusCodeAsGetResponseThenThrowException(int statusCode)
      throws IOException {
    try (var response = DefaultClassicHttpResponseFactory.INSTANCE.newHttpResponse(statusCode,
        "test")) {

      var lambda = blobRestConnectorImpl.downloadFileIn(blobIn);

      assertThrows(ResponseStatusException.class, () -> lambda.handleResponse(response));
    }
  }

  @Test
  void givenPutResponse201ThenReturnsNull() throws HttpException, IOException {
    var response = DefaultClassicHttpResponseFactory.INSTANCE.newHttpResponse(201, "test");

    var value = blobRestConnectorImpl.validateStatusCode().handleResponse(response);

    assertNull(value);
  }

  @ParameterizedTest
  @ValueSource(ints = { 301, 400, 404, 500, 502 })
  void givenBadStatusCodeAsPutResponseThenThrowException(int statusCode)
      throws IOException {
    try (var response = DefaultClassicHttpResponseFactory.INSTANCE.newHttpResponse(statusCode,
        "test")) {

      var lambda = blobRestConnectorImpl.validateStatusCode();

      assertThrows(ResponseStatusException.class, () -> lambda.handleResponse(response));
    }
  }

  @Test
  void shouldSetMetadata(CapturedOutput output) throws IOException {
    BlobApplicationAware blobOut = blobRestConnectorImpl.setMetadata(blobIn);
    verify(client, times(1)).execute(any(HttpPut.class), any(HttpClientResponseHandler.class));
    assertEquals(BlobApplicationAware.Status.ENRICHED, blobOut.getStatus());
    assertThat(output.getOut(), not(containsString("Cannot SET metadata for the blob")));
  }

  @Test
  void shouldNotSetMetadataForWallet(CapturedOutput output) throws IOException {
    blobIn.setApp(Application.WALLET);
    blobIn.setStatus(Status.SPLIT);
    BlobApplicationAware blobOut = blobRestConnectorImpl.setMetadata(blobIn);
    verify(client, times(0)).execute(any(HttpPut.class), any(HttpClientResponseHandler.class));
    assertEquals(BlobApplicationAware.Status.SPLIT, blobOut.getStatus());
  }

  @Test
  void shouldFailSetMetadataHttpError(CapturedOutput output) throws IOException {
    doThrow(new ResponseStatusException(HttpStatusCode.valueOf(404), "not_found"))
        .when(client).execute(any(HttpPut.class), any(HttpClientResponseHandler.class));

    BlobApplicationAware blobOut = blobRestConnectorImpl.setMetadata(blobIn);

    verify(client, times(1)).execute(any(HttpPut.class), any(HttpClientResponseHandler.class));
    assertEquals(BlobApplicationAware.Status.RECEIVED, blobOut.getStatus());
    assertThat(output.getOut(), containsString("Cannot SET metadata for the blob"));
  }
}
