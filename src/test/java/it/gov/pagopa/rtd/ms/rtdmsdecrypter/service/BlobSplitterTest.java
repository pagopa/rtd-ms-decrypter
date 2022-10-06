package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Application;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Status;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ContextConfiguration(classes = {BlobSplitterImpl.class})
@TestPropertySource(value = {"classpath:application-nokafka.yml"}, inheritProperties = false)
class BlobSplitterTest {

  @Autowired
  BlobSplitterImpl blobSplitterImpl;

  @Value("${decrypt.resources.base.path}")
  String resources;

  @Value("${decrypt.resources.base.path}/tmp")
  String tmpDirectory;

  String batchServiceChunkNumber = "01";

  String containerRTD = "rtd-transactions-32489876908u74bh781e2db57k098c5ad00000000000";

  String containerTAE = "ade-transactions-32489876908u74bh781e2db57k098c5ad00000000000";
  String blobNameRTD =
      "CSTAR.99999.TRNLOG.20220419.121045.001." + batchServiceChunkNumber + ".csv";

  String blobNameRTDOldNaming = "CSTAR.99999.TRNLOG.20220419.121045.001.csv";

  String blobNameTAE = "ADE.99999.TRNLOG.20220721.095718.001." + batchServiceChunkNumber + ".csv";

  String blobNameTAEOldNaming =
      "ADE.99999.TRNLOG.20220721.095718.001." + batchServiceChunkNumber + ".csv";

  String blobNameTAEEmpty =
      "ADE.00000.TRNLOG.20220721.095718.001." + batchServiceChunkNumber + ".csv";

  BlobApplicationAware fakeBlobRTD;

  BlobApplicationAware fakeBlobRTDOldNaming;

  BlobApplicationAware fakeBlobTAE;

  BlobApplicationAware fakeBlobTAEOldNaming;

  BlobApplicationAware fakeBlobTAEEmpty;

  @BeforeEach
  void setUp() throws IOException {

    //Create the decrypted file for RTD
    File decryptedFile = Path.of(tmpDirectory, blobNameRTD).toFile();
    decryptedFile.getParentFile().mkdirs();
    decryptedFile.createNewFile();
    FileOutputStream decryptedStream = new FileOutputStream(
        Path.of(tmpDirectory, blobNameRTD + ".decrypted").toString());
    Files.copy(Path.of(resources, blobNameRTD), decryptedStream);

    //Instantiate a fake RTD blob with clear text content
    fakeBlobRTD = new BlobApplicationAware(
        "/blobServices/default/containers/" + containerRTD + "/blobs/" + blobNameRTD);
    fakeBlobRTD.setTargetDir(tmpDirectory);
    fakeBlobRTD.setStatus(Status.DECRYPTED);
    fakeBlobRTD.setApp(Application.RTD);

    //Create the decrypted file for RTD old naming convention
    FileOutputStream decryptedStreamTransactionsOldNaming = new FileOutputStream(
        Path.of(tmpDirectory, blobNameRTDOldNaming + ".decrypted").toString());
    Files.copy(Path.of(resources, blobNameRTDOldNaming), decryptedStreamTransactionsOldNaming);

    //Instantiate a fake RTD blob with old naming convention
    fakeBlobRTDOldNaming = new BlobApplicationAware(
        "/blobServices/default/containers/" + containerRTD + "/blobs/" + blobNameRTDOldNaming);
    fakeBlobRTDOldNaming.setTargetDir(tmpDirectory);
    fakeBlobRTDOldNaming.setStatus(Status.DECRYPTED);
    fakeBlobRTDOldNaming.setApp(Application.RTD);

    //Create the decrypted file for TAE
    File decryptedFileAggregates = Path.of(tmpDirectory, blobNameTAE).toFile();
    decryptedFileAggregates.getParentFile().mkdirs();
    decryptedFileAggregates.createNewFile();
    FileOutputStream decryptedStreamAggregates = new FileOutputStream(
        Path.of(tmpDirectory, blobNameTAE + ".decrypted").toString());
    Files.copy(Path.of(resources, blobNameTAE), decryptedStreamAggregates);

    //Instantiate a fake TAE blob with clear text content
    fakeBlobTAE = new BlobApplicationAware(
        "/blobServices/default/containers/" + containerTAE + "/blobs/" + blobNameTAE);
    fakeBlobTAE.setTargetDir(tmpDirectory);
    fakeBlobTAE.setStatus(Status.DECRYPTED);
    fakeBlobTAE.setApp(Application.ADE);

    //Create the decrypted empty file for TAE
    File decryptedFileAggregatesEmpty = Path.of(tmpDirectory, blobNameTAEEmpty).toFile();
    decryptedFileAggregatesEmpty.getParentFile().mkdirs();
    decryptedFileAggregatesEmpty.createNewFile();
    FileOutputStream decryptedStreamAggregatesEmpty = new FileOutputStream(
        Path.of(tmpDirectory, blobNameTAEEmpty + ".decrypted").toString());
    Files.copy(Path.of(resources, blobNameTAEEmpty), decryptedStreamAggregatesEmpty);

    //Instantiate a fake TAE blob with clear text content
    fakeBlobTAEEmpty = new BlobApplicationAware(
        "/blobServices/default/containers/" + containerTAE + "/blobs/" + blobNameTAEEmpty);
    fakeBlobTAEEmpty.setTargetDir(tmpDirectory);
    fakeBlobTAEEmpty.setStatus(Status.DECRYPTED);
    fakeBlobTAEEmpty.setApp(Application.ADE);

    //Create the decrypted empty file for TAE with old naming convention
    FileOutputStream decryptedStreamAggregatesOldNaming = new FileOutputStream(
        Path.of(tmpDirectory, blobNameTAEOldNaming + ".decrypted").toString());
    Files.copy(Path.of(resources, blobNameTAEOldNaming), decryptedStreamAggregatesOldNaming);

    //Instantiate a fake TAE blob with old naming convention
    fakeBlobTAEOldNaming = new BlobApplicationAware(
        "/blobServices/default/containers/" + containerTAE + "/blobs/" + blobNameTAEOldNaming);
    fakeBlobTAEOldNaming.setTargetDir(tmpDirectory);
    fakeBlobTAEOldNaming.setStatus(Status.DECRYPTED);
    fakeBlobTAEOldNaming.setApp(Application.ADE);
  }

  @AfterEach
  void cleanTmpFiles() throws IOException {
    FileUtils.deleteDirectory(Path.of(tmpDirectory).toFile());
  }

  @Test
  void shouldSplitRTD() {

    blobSplitterImpl.setLineThreshold(1);

    Stream<BlobApplicationAware> chunks = blobSplitterImpl.split(fakeBlobRTD);
    Iterable<BlobApplicationAware> iterable = chunks::iterator;
    int i = 0;
    for (BlobApplicationAware b : iterable) {
      assertEquals(Status.SPLIT, b.getStatus());
      assertEquals(blobNameRTD + "." + i + ".decrypted", b.getBlob());
      i++;
    }
    assertEquals(3, i);
  }

  @Test
  void shouldSplitReminderRTD() {

    blobSplitterImpl.setLineThreshold(2);

    Stream<BlobApplicationAware> chunks = blobSplitterImpl.split(fakeBlobRTD);
    Iterable<BlobApplicationAware> iterable = chunks::iterator;
    int i = 0;
    for (BlobApplicationAware b : iterable) {
      assertEquals(Status.SPLIT, b.getStatus());
      assertEquals(blobNameRTD + "." + i + ".decrypted", b.getBlob());
      i++;
    }
    assertEquals(2, i);
  }

  @Test
  void shouldSplitRTDOldNaming() {

    blobSplitterImpl.setLineThreshold(1);

    Stream<BlobApplicationAware> chunks = blobSplitterImpl.split(fakeBlobRTDOldNaming);
    Iterable<BlobApplicationAware> iterable = chunks::iterator;
    int i = 0;
    for (BlobApplicationAware b : iterable) {
      assertEquals(Status.SPLIT, b.getStatus());
      assertEquals(blobNameRTDOldNaming + "." + i + ".decrypted", b.getBlob());
      i++;
    }
    assertEquals(3, i);
  }

  @Test
  void shouldSplitTAE() {
    //Instantiate a fake blob with clear text content
    blobSplitterImpl.setLineThreshold(1);

    Stream<BlobApplicationAware> chunks = blobSplitterImpl.split(fakeBlobTAE);
    Iterable<BlobApplicationAware> iterable = chunks::iterator;
    int i = 0;
    for (BlobApplicationAware b : iterable) {
      assertEquals(Status.SPLIT, b.getStatus());
      assertEquals("AGGADE." + b.getSenderCode() + "." + b.getFileCreationDate() + "."
          + b.getFileCreationTime() + "." + b.getFlowNumber() + "." + batchServiceChunkNumber + "."
          + i, b.getBlob());
      i++;
    }
    assertEquals(4, i);
  }

  @Test
  void shouldSplitTAEOldNaming() {
    //Instantiate a fake blob with clear text content
    blobSplitterImpl.setLineThreshold(1);
    Stream<BlobApplicationAware> chunks = blobSplitterImpl.split(fakeBlobTAEOldNaming);
    Iterable<BlobApplicationAware> iterable = chunks::iterator;
    int i = 0;
    for (BlobApplicationAware b : iterable) {
      assertEquals(Status.SPLIT, b.getStatus());
      assertEquals("AGGADE." + b.getSenderCode() + "." + b.getFileCreationDate() + "."
              + b.getFileCreationTime() + "." + b.getFlowNumber() + "." + batchServiceChunkNumber + "."
              + i,
          b.getBlob());
      System.err.println(b.getBlob());
      i++;
    }
    assertEquals(4, i);
  }

  //This test, contrary to the previous one, tests the scenario where the file run out of lines
  // before reaching the threshold.
  @Test
  void shouldSplitReminderTAE() {

    blobSplitterImpl.setLineThreshold(2);

    Stream<BlobApplicationAware> chunks = blobSplitterImpl.split(fakeBlobTAE);
    Iterable<BlobApplicationAware> iterable = chunks::iterator;
    int i = 0;
    for (BlobApplicationAware b : iterable) {
      assertEquals(Status.SPLIT, b.getStatus());
      assertEquals("AGGADE." + b.getSenderCode() + "." + b.getFileCreationDate() + "."
          + b.getFileCreationTime() + "." + b.getFlowNumber() + "." + batchServiceChunkNumber + "."
          + i, b.getBlob());
      i++;
    }
    assertEquals(2, i);
  }

  @Test
  void shouldNotSplitMissingFile() {

    //Set the wrong directory for locating the decrypted fake blob
    fakeBlobRTD.setTargetDir("pippo");
    fakeBlobRTD.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    blobSplitterImpl.setLineThreshold(1);

    Stream<BlobApplicationAware> chunks = blobSplitterImpl.split(fakeBlobRTD);
    ArrayList<BlobApplicationAware> originalMissingBlob = (ArrayList<BlobApplicationAware>) chunks.collect(
        Collectors.toList());
    assertEquals(1, originalMissingBlob.size());
    assertEquals(Status.DELETED, originalMissingBlob.get(0).getStatus());
  }

}

