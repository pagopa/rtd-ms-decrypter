package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;
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


  String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad00000000000";
  String blobName = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp";
  BlobApplicationAware fakeBlob = new BlobApplicationAware(
      "/blobServices/default/containers/" + container + "/blobs/" + blobName);

  @Test
  void shouldSplit() throws IOException {

    String transactions = "cleartext.csv";

    FileOutputStream decrypted = new FileOutputStream(
        Path.of(resources, blobName + ".decrypted").toString());
    Files.copy(Path.of(resources, transactions), decrypted);

    fakeBlob.setTargetDir(resources);
    fakeBlob.setStatus(BlobApplicationAware.Status.DOWNLOADED);

    assertEquals(3, blobSplitterImpl.split(fakeBlob).count());

    cleanLocalTestFiles(blobName);
  }

  @Test
  void shouldNotSplitMissingFile(CapturedOutput output) throws IOException {

    String transactions = "cleartext.csv";

    FileOutputStream decrypted = new FileOutputStream(Path.of(resources, blobName).toString());
    Files.copy(Path.of(resources, transactions), decrypted);

    fakeBlob.setTargetDir(resources);
    fakeBlob.setStatus(BlobApplicationAware.Status.DOWNLOADED);

    blobSplitterImpl.split(fakeBlob);
    assertThat(output.getOut(), containsString("Missing blob file:"));

    cleanLocalTestFiles(blobName);
  }

  /**
   * Some tests create local files. This method is called at the end of them to clean up those
   * temporary files, avoiding clogging the resource directory during testing.
   *
   * @param filenames varargs of local file names to be deleted.
   */
  void cleanLocalTestFiles(String... filenames) {
    Pattern p = Pattern.compile(filenames[0] + ".*");
    try {
      for (File f : Objects.requireNonNull(Path.of(resources).toFile().listFiles())) {
        if (p.matcher(f.getName()).matches()) {
          Files.delete(Path.of(resources, f.getName()));
        }
      }
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}

