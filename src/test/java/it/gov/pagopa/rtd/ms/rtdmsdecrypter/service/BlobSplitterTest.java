package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Status;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
  BlobApplicationAware fakeBlob;

  @BeforeEach
  void setUp() throws IOException {

    String transactions = "cleartext.csv";

    //Create the decrypted file
    File decryptedFile = Path.of(tmpDirectory, blobName + ".decrypted").toFile();
    decryptedFile.getParentFile().mkdirs();
    decryptedFile.createNewFile();
    FileOutputStream decryptedStream = new FileOutputStream(
        Path.of(tmpDirectory, blobName + ".decrypted").toString());
    Files.copy(Path.of(resources, transactions), decryptedStream);

    //Instantiate a fake blob with clear text content
    fakeBlob = new BlobApplicationAware(
        "/blobServices/default/containers/" + container + "/blobs/" + blobName);
    fakeBlob.setTargetDir(tmpDirectory);
    fakeBlob.setStatus(Status.DECRYPTED);
  }

  @AfterEach
  void cleanTmpFiles() throws IOException {
    FileUtils.deleteDirectory(Path.of(tmpDirectory).toFile());
  }

  @Test
  void shouldSplit(CapturedOutput output) {

    blobSplitterImpl.setLineThreshold(1);

    Stream<BlobApplicationAware> chunks = blobSplitterImpl.split(fakeBlob);
    Iterable<BlobApplicationAware> iterable = chunks::iterator;
    int i = 0;
    for (BlobApplicationAware b : iterable) {
      assertEquals(Status.SPLIT, b.getStatus());
      assertEquals(blobName + "." + i + ".decrypted", b.getBlob());
      i++;
    }
    assertEquals(3, i);
    assertThat(output.getOut(), containsString("Obtained 3 chunk/s from blob:"));

  }

  //This test, contrary to the previous one, tests the scenario where the file run out of lines
  // before reaching the threshold.
  @Test
  void shouldSplitReminder(CapturedOutput output) {

    blobSplitterImpl.setLineThreshold(2);

    Stream<BlobApplicationAware> chunks = blobSplitterImpl.split(fakeBlob);
    Iterable<BlobApplicationAware> iterable = chunks::iterator;
    int i = 0;
    for (BlobApplicationAware b : iterable) {
      assertEquals(Status.SPLIT, b.getStatus());
      assertEquals(blobName + "." + i + ".decrypted", b.getBlob());
      i++;
    }
    assertEquals(2, i);
    assertThat(output.getOut(), containsString("Obtained 2 chunk/s from blob:"));

  }

  @Test
  void shouldNotSplitMissingFile(CapturedOutput output) {

    //Set the wrong directory for locating the decrypted fake blob
    fakeBlob.setTargetDir(resources);
    fakeBlob.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    blobSplitterImpl.setLineThreshold(1);

    blobSplitterImpl.split(fakeBlob);
    assertThat(output.getOut(), containsString("Missing blob file:"));

  }

}

