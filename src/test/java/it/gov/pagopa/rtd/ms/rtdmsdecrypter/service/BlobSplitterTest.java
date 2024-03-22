package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

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

  String missingBatchServiceChunkNumberPlaceholder = "00";

  String batchServiceChunkNumber = "01";

  String containerRTD = "rtd-transactions-32489876908u74bh781e2db57k098c5ad00000000000";

  String containerTAE = "ade-transactions-32489876908u74bh781e2db57k098c5ad00000000000";

  String containerWallet = "nexi";

  String contractsFolder = "in";

  String blobNameRTD =
      "CSTAR.99999.TRNLOG.20220419.121045.001." + batchServiceChunkNumber + ".csv";

  String blobNameRTDOldNaming = "CSTAR.99999.TRNLOG.20220419.121045.001.csv";

  String blobNameTAE = "ADE.99999.TRNLOG.20220721.095718.001." + batchServiceChunkNumber + ".csv";

  String blobNameTAEOldNaming =
      "ADE.99999.TRNLOG.20220721.095718.001.csv";

  String blobNameTAEEmpty =
      "ADE.00000.TRNLOG.20220721.095718.001." + batchServiceChunkNumber + ".csv";

  String blobNameWallet = "PAGOPAPM_NPG_CONTRACTS_20240323000000_001_OUT.decrypted";

  String blobNameWalletMalformed = "PAGOPAPM_NPG_CONTRACTS_20240318000000_001_OUT.decrypted";

  String blobNameWalletMalformedJson = "PAGOPAPM_NPG_CONTRACTS_20240321000000_001_OUT.decrypted";

  String blobNameWalletNoContracts = "PAGOPAPM_NPG_CONTRACTS_20240319000000_001_OUT.decrypted";

  String blobNameWalletNoArrayContracts = "PAGOPAPM_NPG_CONTRACTS_20240320000000_001_OUT.decrypted";

  String blobNameWalletNoHeader = "PAGOPAPM_NPG_CONTRACTS_20240322000000_001_OUT.decrypted";

  String blobNameWalletMissing = "PAGOPAPM_NPG_CONTRACTS_20240322000000_002_OUT.decrypted";

  BlobApplicationAware fakeBlobRTD;

  BlobApplicationAware fakeBlobRTDOldNaming;

  BlobApplicationAware fakeBlobTAE;

  BlobApplicationAware fakeBlobTAEOldNaming;

  BlobApplicationAware fakeBlobTAEEmpty;

  BlobApplicationAware fakeBlobWallet;

  BlobApplicationAware malformedBlobWallet;

  BlobApplicationAware malformedJsonBlobWallet;

  BlobApplicationAware noContractsBlobWallet;

  BlobApplicationAware noArrayContractsBlobWallet;

  BlobApplicationAware noHeaderBlobWallet;

  BlobApplicationAware missingBlobWallet;

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

    //Create the decrypted file for Wallet migration
    File decryptedExportFile = Path.of(tmpDirectory, blobNameWallet).toFile();
    decryptedExportFile.getParentFile().mkdirs();
    decryptedExportFile.createNewFile();
    FileOutputStream decryptedWalletStream = new FileOutputStream(
        Path.of(tmpDirectory, blobNameWallet + ".decrypted").toString());
    Files.copy(Path.of(resources, blobNameWallet), decryptedWalletStream);

    //Instantiate a fake Wallet blob with clear text content
    String blobUri =
        "/blobServices/default/containers/" + containerWallet + "/blobs/" + contractsFolder + "/"
            + blobNameWallet;

    fakeBlobWallet = new BlobApplicationAware(blobUri);
    fakeBlobWallet.setTargetDir(tmpDirectory);
    fakeBlobWallet.setStatus(Status.DECRYPTED);
    fakeBlobWallet.setApp(Application.WALLET);

    //Create a malformed decrypted file for Wallet migration
    File malformedDecryptedExportFile = Path.of(tmpDirectory, blobNameWalletMalformed).toFile();
    malformedDecryptedExportFile.getParentFile().mkdirs();
    malformedDecryptedExportFile.createNewFile();
    FileOutputStream malformedDecryptedWalletStream = new FileOutputStream(
        Path.of(tmpDirectory, blobNameWalletMalformed + ".decrypted").toString());
    Files.copy(Path.of(resources, blobNameWalletMalformed), malformedDecryptedWalletStream);

    //Instantiate a fake Wallet blob with clear text content
    malformedBlobWallet = new BlobApplicationAware(
        "/blobServices/default/containers/" + containerWallet + "/blobs/" + contractsFolder + "/"
            + blobNameWalletMalformed);
    malformedBlobWallet.setTargetDir(tmpDirectory);
    malformedBlobWallet.setStatus(Status.DECRYPTED);
    malformedBlobWallet.setApp(Application.WALLET);

    //Create a malformed decrypted file for Wallet migration
    File malformedJsonDecryptedExportFile = Path.of(tmpDirectory, blobNameWalletMalformedJson).toFile();
    malformedJsonDecryptedExportFile.getParentFile().mkdirs();
    malformedJsonDecryptedExportFile.createNewFile();
    FileOutputStream malformedJsonDecryptedWalletStream = new FileOutputStream(
        Path.of(tmpDirectory, blobNameWalletMalformedJson + ".decrypted").toString());
    Files.copy(Path.of(resources, blobNameWalletMalformedJson), malformedJsonDecryptedWalletStream);

    //Instantiate a fake Wallet blob with clear text content
    malformedJsonBlobWallet = new BlobApplicationAware(
        "/blobServices/default/containers/" + containerWallet + "/blobs/" + contractsFolder + "/"
            + blobNameWalletMalformedJson);
    malformedJsonBlobWallet.setTargetDir(tmpDirectory);
    malformedJsonBlobWallet.setStatus(Status.DECRYPTED);
    malformedJsonBlobWallet.setApp(Application.WALLET);

    //Create a decrypted file for Wallet migration without contracts array
    File noContractsDecryptedExportFile = Path.of(tmpDirectory, blobNameWalletNoContracts).toFile();
    noContractsDecryptedExportFile.getParentFile().mkdirs();
    noContractsDecryptedExportFile.createNewFile();
    FileOutputStream noContractsdDecryptedWalletStream = new FileOutputStream(
        Path.of(tmpDirectory, blobNameWalletNoContracts + ".decrypted").toString());
    Files.copy(Path.of(resources, blobNameWalletNoContracts), noContractsdDecryptedWalletStream);

    //Instantiate a fake no-contracts Wallet blob with clear text content
    noContractsBlobWallet = new BlobApplicationAware(
        "/blobServices/default/containers/" + containerWallet + "/blobs/" + contractsFolder + "/"
            + blobNameWalletNoContracts);
    noContractsBlobWallet.setTargetDir(tmpDirectory);
    noContractsBlobWallet.setStatus(Status.DECRYPTED);
    noContractsBlobWallet.setApp(Application.WALLET);

    //Create a decrypted file for Wallet migration without contracts as array
    File noArrayContractsDecryptedExportFile = Path.of(tmpDirectory, blobNameWalletNoArrayContracts).toFile();
    noArrayContractsDecryptedExportFile.getParentFile().mkdirs();
    noArrayContractsDecryptedExportFile.createNewFile();
    FileOutputStream noArrayContractsdDecryptedWalletStream = new FileOutputStream(
        Path.of(tmpDirectory, blobNameWalletNoArrayContracts + ".decrypted").toString());
    Files.copy(Path.of(resources, blobNameWalletNoArrayContracts), noArrayContractsdDecryptedWalletStream);

    //Instantiate a fake no-contracts Wallet blob with clear text content
    noArrayContractsBlobWallet = new BlobApplicationAware(
        "/blobServices/default/containers/" + containerWallet + "/blobs/" + contractsFolder + "/"
            + blobNameWalletNoArrayContracts);
    noArrayContractsBlobWallet.setTargetDir(tmpDirectory);
    noArrayContractsBlobWallet.setStatus(Status.DECRYPTED);
    noArrayContractsBlobWallet.setApp(Application.WALLET);

    //Create a decrypted file for Wallet migration without header
    File noHeadersDecryptedExportFile = Path.of(tmpDirectory, blobNameWalletNoHeader).toFile();
    noHeadersDecryptedExportFile.getParentFile().mkdirs();
    noHeadersDecryptedExportFile.createNewFile();
    FileOutputStream noHeaderDecryptedWalletStream = new FileOutputStream(
        Path.of(tmpDirectory, blobNameWalletNoHeader + ".decrypted").toString());
    Files.copy(Path.of(resources, blobNameWalletNoHeader), noHeaderDecryptedWalletStream);

    //Instantiate a fake no-header Wallet blob with clear text content
    noHeaderBlobWallet = new BlobApplicationAware(
        "/blobServices/default/containers/" + containerWallet + "/blobs/" + contractsFolder + "/"
            + blobNameWalletNoHeader);
    noHeaderBlobWallet.setTargetDir(tmpDirectory);
    noHeaderBlobWallet.setStatus(Status.DECRYPTED);
    noHeaderBlobWallet.setApp(Application.WALLET);

    //Instantiate a fake missing Wallet blob
    missingBlobWallet = new BlobApplicationAware(
        "/blobServices/default/containers/" + containerWallet + "/blobs/" + contractsFolder + "/"
            + blobNameWalletMissing);
    missingBlobWallet.setTargetDir(tmpDirectory);
    missingBlobWallet.setStatus(Status.DECRYPTED);
    missingBlobWallet.setApp(Application.WALLET);
  }

  @AfterEach
  void cleanTmpFiles() throws IOException {
    FileUtils.deleteDirectory(Path.of(tmpDirectory).toFile());
  }

  @Test
  void shouldSplitRTD() {

    blobSplitterImpl.setAggregatesLineThreshold(1);
    blobSplitterImpl.setChecksumSkipped(false);

    Stream<BlobApplicationAware> chunks = blobSplitterImpl.split(fakeBlobRTD);
    Iterable<BlobApplicationAware> iterable = chunks::iterator;
    int i = 0;
    for (BlobApplicationAware b : iterable) {
      assertEquals(Status.SPLIT, b.getStatus());
      assertEquals(blobNameRTD + "." + i + ".decrypted", b.getBlob());
      assertEquals(b.getOriginalFileChunksNumber(), 3);
      i++;
    }
    assertEquals(3, i);
  }

  @Test
  void shouldSplitWallet() {

    blobSplitterImpl.setContractsSplitThreshold(1);

    Stream<BlobApplicationAware> chunks = blobSplitterImpl.split(fakeBlobWallet);
    Iterable<BlobApplicationAware> iterable = chunks::iterator;
    int i = 0;
    for (BlobApplicationAware b : iterable) {
      assertEquals(Status.SPLIT, b.getStatus());
      assertEquals(blobNameWallet + "." + i + ".decrypted", b.getBlob());
      assertEquals(b.getOriginalFileChunksNumber(), 4);
      i++;
    }
    assertEquals(4, i);
  }

  @Test
  void shouldNotSplitWalletMalformed() {
    blobSplitterImpl.setContractsSplitThreshold(1);

    Stream<BlobApplicationAware> chunks = blobSplitterImpl.split(malformedBlobWallet);
    Iterable<BlobApplicationAware> iterable = chunks::iterator;
    int i = 0;
    for (BlobApplicationAware b : iterable) {
      assertEquals(Status.DELETED, b.getStatus());
      assertEquals(blobNameWalletMalformed, b.getBlob());
      i++;
    }
    assertEquals(1, i);
  }

  @Test
  void shouldNotSplitWalletMalformedJson() {
    blobSplitterImpl.setContractsSplitThreshold(1);

    Stream<BlobApplicationAware> chunks = blobSplitterImpl.split(malformedJsonBlobWallet);
    Iterable<BlobApplicationAware> iterable = chunks::iterator;
    int i = 0;
    for (BlobApplicationAware b : iterable) {
      assertEquals(Status.DELETED, b.getStatus());
      assertEquals(blobNameWalletMalformedJson, b.getBlob());
      i++;
    }
    assertEquals(1, i);
  }

  @Test
  void shouldNotSplitWalletNoContracts() {
    blobSplitterImpl.setContractsSplitThreshold(1);

    Stream<BlobApplicationAware> chunks = blobSplitterImpl.split(noContractsBlobWallet);
    Iterable<BlobApplicationAware> iterable = chunks::iterator;
    int i = 0;
    for (BlobApplicationAware b : iterable) {
      assertEquals(Status.DELETED, b.getStatus());
      assertEquals(blobNameWalletNoContracts, b.getBlob());
      i++;
    }
    assertEquals(1, i);
  }

  @Test
  void shouldNotSplitWalletNoArrayContracts() {
    blobSplitterImpl.setContractsSplitThreshold(1);

    Stream<BlobApplicationAware> chunks = blobSplitterImpl.split(noArrayContractsBlobWallet);
    Iterable<BlobApplicationAware> iterable = chunks::iterator;
    int i = 0;
    for (BlobApplicationAware b : iterable) {
      assertEquals(Status.DELETED, b.getStatus());
      assertEquals(blobNameWalletNoArrayContracts, b.getBlob());
      i++;
    }
    assertEquals(1, i);
  }

  @Test
  void shouldNotSplitWalletNoHeader() {
    blobSplitterImpl.setContractsSplitThreshold(1);

    Stream<BlobApplicationAware> chunks = blobSplitterImpl.split(noHeaderBlobWallet);
    Iterable<BlobApplicationAware> iterable = chunks::iterator;
    int i = 0;
    for (BlobApplicationAware b : iterable) {
      assertEquals(Status.DELETED, b.getStatus());
      assertEquals(blobNameWalletNoHeader, b.getBlob());
      i++;
    }
    assertEquals(1, i);
  }

  @Test
  void shouldNotSplitWalletMissing() {
    blobSplitterImpl.setContractsSplitThreshold(1);

    Stream<BlobApplicationAware> chunks = blobSplitterImpl.split(missingBlobWallet);
    Iterable<BlobApplicationAware> iterable = chunks::iterator;
    int i = 0;
    for (BlobApplicationAware b : iterable) {
      assertEquals(Status.DELETED, b.getStatus());
      assertEquals(blobNameWalletMissing, b.getBlob());
      i++;
    }
    assertEquals(1, i);
  }

  @Test
  void shouldSplitWalletWithReminder() {

    blobSplitterImpl.setContractsSplitThreshold(3);

    Stream<BlobApplicationAware> chunks = blobSplitterImpl.split(fakeBlobWallet);
    Iterable<BlobApplicationAware> iterable = chunks::iterator;
    int i = 0;
    for (BlobApplicationAware b : iterable) {
      assertEquals(Status.SPLIT, b.getStatus());
      assertEquals(blobNameWallet + "." + i + ".decrypted", b.getBlob());
      assertEquals(b.getOriginalFileChunksNumber(), 2);
      i++;
    }
    assertEquals(2, i);
  }

  @Test
  void shouldSplitRTDNotSkippingChecksum() {

    blobSplitterImpl.setAggregatesLineThreshold(1);
    blobSplitterImpl.setChecksumSkipped(true);

    Stream<BlobApplicationAware> chunks = blobSplitterImpl.split(fakeBlobRTD);
    Iterable<BlobApplicationAware> iterable = chunks::iterator;
    int i = 0;
    for (BlobApplicationAware b : iterable) {
      assertEquals(Status.SPLIT, b.getStatus());
      assertEquals(blobNameRTD + "." + i + ".decrypted", b.getBlob());
      assertEquals(b.getOriginalFileChunksNumber(), 4);
      i++;
    }
    assertEquals(4, i);
  }

  @Test
  void shouldSplitReminderRTD() {

    blobSplitterImpl.setAggregatesLineThreshold(2);
    blobSplitterImpl.setChecksumSkipped(false);

    Stream<BlobApplicationAware> chunks = blobSplitterImpl.split(fakeBlobRTD);
    Iterable<BlobApplicationAware> iterable = chunks::iterator;
    int i = 0;
    for (BlobApplicationAware b : iterable) {
      assertEquals(Status.SPLIT, b.getStatus());
      assertEquals(blobNameRTD + "." + i + ".decrypted", b.getBlob());
      assertEquals(b.getOriginalFileChunksNumber(), 2);
      i++;
    }
    assertEquals(2, i);
  }

  @Test
  void shouldSplitRTDOldNaming() {

    blobSplitterImpl.setAggregatesLineThreshold(1);
    blobSplitterImpl.setChecksumSkipped(false);

    Stream<BlobApplicationAware> chunks = blobSplitterImpl.split(fakeBlobRTDOldNaming);
    Iterable<BlobApplicationAware> iterable = chunks::iterator;
    int i = 0;
    for (BlobApplicationAware b : iterable) {
      assertEquals(Status.SPLIT, b.getStatus());
      assertEquals(blobNameRTDOldNaming + "." + i + ".decrypted", b.getBlob());
      assertEquals(b.getOriginalFileChunksNumber(), 3);
      i++;
    }
    assertEquals(3, i);
  }

  @Test
  void shouldSplitTAE() {
    //Instantiate a fake blob with clear text content

    blobSplitterImpl.setAggregatesLineThreshold(1);
    blobSplitterImpl.setChecksumSkipped(false);
    Stream<BlobApplicationAware> chunks = blobSplitterImpl.split(fakeBlobTAE);
    Iterable<BlobApplicationAware> iterable = chunks::iterator;
    int i = 0;
    for (BlobApplicationAware b : iterable) {
      assertEquals(Status.SPLIT, b.getStatus());
      assertEquals("AGGADE." + b.getSenderCode() + "." + b.getFileCreationDate() + "."
          + b.getFileCreationTime() + "." + b.getFlowNumber() + "." + batchServiceChunkNumber
          + String.format("%03d", i), b.getBlob());
      assertEquals(b.getOriginalFileChunksNumber(), 6);
      i++;
    }
    assertEquals(6, i);
  }

  @Test
  void shouldSplitTAEOldNaming() {
    //Instantiate a fake blob with clear text content
    blobSplitterImpl.setAggregatesLineThreshold(1);
    blobSplitterImpl.setChecksumSkipped(false);

    Stream<BlobApplicationAware> chunks = blobSplitterImpl.split(fakeBlobTAEOldNaming);
    Iterable<BlobApplicationAware> iterable = chunks::iterator;
    int i = 0;
    for (BlobApplicationAware b : iterable) {
      assertEquals(Status.SPLIT, b.getStatus());
      assertEquals("AGGADE." + b.getSenderCode() + "." + b.getFileCreationDate() + "."
              + b.getFileCreationTime() + "." + b.getFlowNumber() + "."
              + missingBatchServiceChunkNumberPlaceholder
              + String.format("%03d", i),
          b.getBlob());
      assertEquals(b.getOriginalFileChunksNumber(), 4);
      i++;
    }
    assertEquals(4, i);
  }

  //This test, contrary to the previous one, tests the scenario where the file run out of lines
  // before reaching the threshold.
  @Test
  void shouldSplitReminderTAE() {

    blobSplitterImpl.setAggregatesLineThreshold(2);
    blobSplitterImpl.setChecksumSkipped(false);

    Stream<BlobApplicationAware> chunks = blobSplitterImpl.split(fakeBlobTAE);
    Iterable<BlobApplicationAware> iterable = chunks::iterator;
    int i = 0;
    for (BlobApplicationAware b : iterable) {
      assertEquals(Status.SPLIT, b.getStatus());
      assertEquals("AGGADE." + b.getSenderCode() + "." + b.getFileCreationDate() + "."
          + b.getFileCreationTime() + "." + b.getFlowNumber() + "." + batchServiceChunkNumber
          + String.format("%03d", i), b.getBlob());
      assertEquals(b.getOriginalFileChunksNumber(), 3);
      i++;
    }
    assertEquals(3, i);
  }

  @Test
  void shouldNotSplitMissingFile() {

    //Set the wrong directory for locating the decrypted fake blob
    fakeBlobRTD.setTargetDir("pippo");
    fakeBlobRTD.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    blobSplitterImpl.setAggregatesLineThreshold(1);
    blobSplitterImpl.setChecksumSkipped(false);

    Stream<BlobApplicationAware> chunks = blobSplitterImpl.split(fakeBlobRTD);
    ArrayList<BlobApplicationAware> originalMissingBlob = (ArrayList<BlobApplicationAware>) chunks.collect(
        Collectors.toList());
    assertEquals(1, originalMissingBlob.size());
    assertEquals(Status.DELETED, originalMissingBlob.get(0).getStatus());
  }

}

