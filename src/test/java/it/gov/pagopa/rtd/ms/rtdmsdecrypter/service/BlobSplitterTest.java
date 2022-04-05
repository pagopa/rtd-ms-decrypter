package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import static it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.TestFilesCleanup.cleanup;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ContextConfiguration(classes = {BlobSplitterImpl.class})
@TestPropertySource(value = {"classpath:application-nokafka.yml"}, inheritProperties = false)
@ExtendWith(OutputCaptureExtension.class)
class BlobSplitterTest {

  @Autowired
  BlobSplitterImpl blobSplitterImpl;

  @Value("${decrypt.resources.base.path}")
  String resources;

  @Value("${decrypt.resources.base.path}/tmp")
  String tmpDirectory;

  String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad00000000000";
  String blobName = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp";
  BlobApplicationAware fakeBlob = new BlobApplicationAware(
      "/blobServices/default/containers/" + container + "/blobs/" + blobName);

  @AfterEach
  void cleanTmpFiles() {
    cleanup(Path.of("src/test/resources/tmp"));
  }

  @Test
  void shouldSplit(CapturedOutput output) throws IOException {

    String transactions = "cleartext.csv";

    FileOutputStream decrypted = new FileOutputStream(
        Path.of(tmpDirectory, blobName + ".decrypted").toString());
    Files.copy(Path.of(resources, transactions), decrypted);

    fakeBlob.setTargetDir(tmpDirectory);
    fakeBlob.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    blobSplitterImpl.setLineThreshold(1);

    assertEquals(3, blobSplitterImpl.split(fakeBlob).count());
    assertThat(output.getOut(), containsString("Obtained 3 chunk/s from blob:"));

  }

  //This test, contrary to the previous one, tests the scenario where the file run out of lines
  // before reaching the threshold.
  @Test
  void shouldSplitReminder(CapturedOutput output) throws IOException {

    String transactions = "cleartext.csv";

    FileOutputStream decrypted = new FileOutputStream(
        Path.of(tmpDirectory, blobName + ".decrypted").toString());
    Files.copy(Path.of(resources, transactions), decrypted);

    fakeBlob.setTargetDir(tmpDirectory);
    fakeBlob.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    blobSplitterImpl.setLineThreshold(2);

    assertEquals(2, blobSplitterImpl.split(fakeBlob).count());
    assertThat(output.getOut(), containsString("Obtained 2 chunk/s from blob:"));

  }

  @Test
  void shouldNotSplitMissingFile(CapturedOutput output) throws IOException {

    String transactions = "cleartext.csv";

    FileOutputStream decrypted = new FileOutputStream(Path.of(tmpDirectory, blobName).toString());
    Files.copy(Path.of(resources, transactions), decrypted);

    fakeBlob.setTargetDir(tmpDirectory);
    fakeBlob.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    blobSplitterImpl.setLineThreshold(1);

    blobSplitterImpl.split(fakeBlob);
    assertThat(output.getOut(), containsString("Missing blob file:"));

  }

}

