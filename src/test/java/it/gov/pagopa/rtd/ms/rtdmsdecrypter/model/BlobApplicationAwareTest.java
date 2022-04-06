package it.gov.pagopa.rtd.ms.rtdmsdecrypter.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Status;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class BlobApplicationAwareTest {

  @Value("${decrypt.resources.base.path}")
  String resources;

  @Value("${decrypt.resources.base.path}/tmp")
  String tmpDirectory;

  String containerRtd = "rtd-transactions-32489876908u74bh781e2db57k098c5ad00000000000";
  String blobNameRtd = "CSTAR.99910.TRNLOG.20220316.164707.001.csv.pgp";

  BlobApplicationAware fakeBlob;

  @BeforeEach
  void setUp() throws IOException {
    //Create dummy files to be deleted
    File encryptedBlob = Path.of(tmpDirectory, blobNameRtd).toFile();
    encryptedBlob.getParentFile().mkdirs();
    encryptedBlob.createNewFile();

    File decryptedBlob = Path.of(tmpDirectory, blobNameRtd + ".decrypted").toFile();
    encryptedBlob.getParentFile().mkdirs();
    encryptedBlob.createNewFile();

    FileOutputStream encryptedBlobStream = new FileOutputStream(
        Path.of(tmpDirectory, blobNameRtd).toString());
    FileOutputStream decryptedBlobStream = new FileOutputStream(
        Path.of(tmpDirectory, blobNameRtd + ".decrypted").toString());

    //Instantiate a fake blob with empty content
    fakeBlob = new BlobApplicationAware(
        "/blobServices/default/containers/" + containerRtd + "/blobs/" + blobNameRtd);
    fakeBlob.setTargetDir(tmpDirectory);
    fakeBlob.setStatus(BlobApplicationAware.Status.DOWNLOADED);
  }

  @AfterEach
  void cleanTmpFiles() throws IOException {
    FileUtils.deleteDirectory(Path.of(tmpDirectory).toFile());
  }

  @Test
  void shouldMatchRegexRTD() {
    String blobUri = "/blobServices/default/containers/" + containerRtd + "/blobs/" + blobNameRtd;
    BlobApplicationAware myBlob = new BlobApplicationAware(blobUri);
    assertSame(BlobApplicationAware.Application.RTD, myBlob.getApp());
  }

  @Test
  void shouldMatchRegexADE() {
    String containerAdE = "ade-transactions-xxxxxxxxxx8u74bh781e2db57k098c5ad034341i8u7y";
    String blobAdE = "ADE.45678.TRNLOG.20220228.103107.001.csv.pgp";
    String blobUri = "/blobServices/default/containers/" + containerAdE + "/blobs/" + blobAdE;
    BlobApplicationAware myBlob = new BlobApplicationAware(blobUri);
    assertSame(BlobApplicationAware.Application.ADE, myBlob.getApp());
  }

  @ParameterizedTest
  @ValueSource(strings = {"/blobServices/default/containers/myContainer/blobs/myBlob",
      "/blobServices/default/directories/myContainer/blobs/myBlob",
      "/blobServices/default/containers/rtd-loremipsum-32489876908u74bh781e2db57k098c5ad034341i8u7y/blobs/CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp"})
  void shouldMatchNoApp(String blobUri) {
    BlobApplicationAware myBlob = new BlobApplicationAware(blobUri);
    assertSame(BlobApplicationAware.Application.NOAPP, myBlob.getApp());
  }

  @Test
  void shouldCleanLocalFiles() {
    assertEquals(Status.DELETED, fakeBlob.localCleanup().getStatus());
  }

  @Test
  void shouldFailFindingLocalEncryptedFile(CapturedOutput output) {
    String blobName = "CSTAR.99910.TRNLOG.20220316.164707.001.csv.pgp.missing";
    fakeBlob.setBlob(blobName);

    fakeBlob.localCleanup();
    assertThat(output.getOut(), containsString("Failed to delete local blob file:"));
  }

  @Test
  void shouldFailFindingLocalDecryptedFile(CapturedOutput output) {
    String blobName = "CSTAR.99910.TRNLOG.20220316.164707.001.csv.pgp.missing";
    fakeBlob.setBlob(blobName);

    fakeBlob.localCleanup();
    assertThat(output.getOut(), containsString("Failed to delete local blob file:"));
  }

}
