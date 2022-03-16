package it.gov.pagopa.rtd.ms.rtdmsdecrypter.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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

  @Test
  void shouldMatchRegexRTD() {
    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp";
    String blobUri = "/blobServices/default/containers/" + container + "/blobs/" + blob;
    BlobApplicationAware myBlob = new BlobApplicationAware(blobUri);
    assertSame(BlobApplicationAware.Application.RTD, myBlob.getApp());
  }

  @Test
  void shouldMatchRegexADE() {
    String container = "ade-transactions-xxxxxxxxxx8u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "ADE.45678.TRNLOG.20220228.103107.001.csv.pgp";
    String blobUri = "/blobServices/default/containers/" + container + "/blobs/" + blob;
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
  void shouldCleanLocalFiles() throws IOException {
    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad00000000000";
    String blobName = "CSTAR.99910.TRNLOG.20220316.164707.001.csv.pgp";

    //Create dummy files to be deleted
    FileOutputStream encryptedBlob = new FileOutputStream(resources + "/" + blobName);
    FileOutputStream decryptedBlob = new FileOutputStream(
        resources + "/" + blobName + ".decrypted");

    BlobApplicationAware fakeBlob = new BlobApplicationAware(
        "/blobServices/default/containers/" + container + "/blobs/" + blobName);
    fakeBlob.setTargetDir(resources);
    fakeBlob.setStatus(BlobApplicationAware.Status.DOWNLOADED);

    assertFalse(fakeBlob.localCleanup());
  }

  @Test
  void shouldFailFindingLocalEncryptedFile(CapturedOutput output) throws FileNotFoundException {
    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad00000000000";
    String blobName = "CSTAR.99910.TRNLOG.20220316.164707.001.csv.pgp";

    //Create dummy file to be deleted
    FileOutputStream decryptedBlob = new FileOutputStream(
        resources + "/" + blobName + ".decrypted");

    BlobApplicationAware fakeBlob = new BlobApplicationAware(
        "/blobServices/default/containers/" + container + "/blobs/" + blobName);
    fakeBlob.setTargetDir(resources);
    fakeBlob.setStatus(BlobApplicationAware.Status.DOWNLOADED);

    fakeBlob.localCleanup();
    assertThat(output.getOut(), containsString("Local blob file missing for deletion:"));
  }

  @Test
  void shouldFailFindingLocalDecryptedFile(CapturedOutput output) throws FileNotFoundException {
    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad00000000000";
    String blobName = "CSTAR.99910.TRNLOG.20220316.164707.001.csv.pgp";

    //Create dummy file to be deleted
    FileOutputStream encryptedBlob = new FileOutputStream(resources + "/" + blobName);

    BlobApplicationAware fakeBlob = new BlobApplicationAware(
        "/blobServices/default/containers/" + container + "/blobs/" + blobName);
    fakeBlob.setTargetDir(resources);
    fakeBlob.setStatus(BlobApplicationAware.Status.DOWNLOADED);

    fakeBlob.localCleanup();
    assertThat(output.getOut(), containsString("Local blob file missing for deletion:"));
  }

}
