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
import java.nio.file.StandardOpenOption;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ContextConfiguration(classes = {BlobVerifierImpl.class})
@TestPropertySource(value = {"classpath:application-nokafka.yml"}, inheritProperties = false)
class BlobVerifierTest {

  @Autowired
  BlobVerifierImpl blobVerifierImpl;

  @Value("${decrypt.resources.base.path}")
  String resources;

  @Value("${decrypt.resources.base.path}/tmp")
  String tmpDirectory;

  String containerRTD = "rtd-transactions-32489876908u74bh781e2db57k098c5ad00000000000";

  String containerTAE = "ade-transactions-32489876908u74bh781e2db57k098c5ad00000000000";

  String blobNameRTD = "CSTAR.99999.TRNLOG.20220419.121045.001.csv";

  String blobNameTAE = "ADE.99999.TRNLOG.20220721.095718.001.csv";

  String blobNameTAEEmpty = "ADE.00000.TRNLOG.20220721.095718.001.csv";

  String blobNameRTDEmpty = "CSTAR.00000.TRNLOG.20220419.121045.001.csv";

  BlobApplicationAware fakeBlobRTD;

  BlobApplicationAware fakeBlobTAE;

  BlobApplicationAware fakeBlobTAEEmpty;

  BlobApplicationAware fakeBlobRTDEmpty;

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
        "/blobServices/default/containers/" + containerRTD + "/blobs/" + blobNameRTD + ".decrypted");
    fakeBlobRTD.setTargetDir(tmpDirectory);
    fakeBlobRTD.setStatus(Status.DECRYPTED);
    fakeBlobRTD.setApp(Application.RTD);

    //Create the decrypted file for TAE
    File decryptedFileAggregates = Path.of(tmpDirectory, blobNameTAE).toFile();
    decryptedFileAggregates.getParentFile().mkdirs();
    decryptedFileAggregates.createNewFile();
    FileOutputStream decryptedStreamAggregates = new FileOutputStream(
        Path.of(tmpDirectory, blobNameTAE + ".decrypted").toString());
    Files.copy(Path.of(resources, blobNameTAE), decryptedStreamAggregates);

    //Instantiate a fake TAE blob with clear text content
    fakeBlobTAE = new BlobApplicationAware(
        "/blobServices/default/containers/" + containerTAE + "/blobs/" + blobNameTAE + ".decrypted");
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
        "/blobServices/default/containers/" + containerTAE + "/blobs/" + blobNameTAEEmpty
            + ".decrypted");
    fakeBlobTAEEmpty.setTargetDir(tmpDirectory);
    fakeBlobTAEEmpty.setStatus(Status.DECRYPTED);
    fakeBlobTAEEmpty.setApp(Application.ADE);

    //Create the decrypted empty file for RTD
    File decryptedFileTransactionsEmpty = Path.of(tmpDirectory, blobNameRTDEmpty).toFile();
    decryptedFileTransactionsEmpty.getParentFile().mkdirs();
    decryptedFileTransactionsEmpty.createNewFile();
    FileOutputStream decryptedStreamTransactionsEmpty = new FileOutputStream(
        Path.of(tmpDirectory, blobNameRTDEmpty + ".decrypted").toString());
    Files.copy(Path.of(resources, blobNameRTDEmpty), decryptedStreamTransactionsEmpty);

    //Instantiate a fake TAE blob with clear text content
    fakeBlobRTDEmpty = new BlobApplicationAware(
        "/blobServices/default/containers/" + containerRTD + "/blobs/" + blobNameRTDEmpty
            + ".decrypted");
    fakeBlobRTDEmpty.setTargetDir(tmpDirectory);
    fakeBlobRTDEmpty.setStatus(Status.DECRYPTED);
    fakeBlobRTDEmpty.setApp(Application.RTD);
  }

  @AfterEach
  void cleanTmpFiles() throws IOException {
    FileUtils.deleteDirectory(Path.of(tmpDirectory).toFile());
  }

  @Test
  void shouldSplitRTD() {
    blobVerifierImpl.verify(fakeBlobRTD);
    assertEquals(Status.VERIFIED, fakeBlobRTD.getStatus());
  }

  @Test
  void shouldSplitTAE() {
    blobVerifierImpl.verify(fakeBlobTAE);
    assertEquals(Status.VERIFIED, fakeBlobTAE.getStatus());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "",
      "\n",
      "00000;00;2022-07-21;2022-07-21;77;249135;978;99999;8894738909374375872;4759769053262163701;00000000003;00000000003;00\n"
          + "\n"
          + "00000;00;2022-07-21;2022-07-20;17;249135;978;99999;8894738909374375872;4759769053262163701;00000000005;00000000005;00",
      ";00;2022-07-21;2022-07-21;77;249135;978;99999;8894738909374375872;4759769053262163701;00000000003;00000000003;00\n",
      "00000;;2022-07-21;2022-07-21;77;249135;978;00000;8894738909374375872;4759769053262163701;00000000003;00000000003;00\n",
      "00000;001;2022-07-21;2022-07-21;77;249135;978;00000;8894738909374375872;4759769053262163701;00000000003;00000000003;00\n",
      "00000;00;;2022-07-21;77;249135;978;00000;8894738909374375872;4759769053262163701;00000000003;00000000003;00\n",
      "00000;00;2022-07-;2022-07-21;77;249135;978;00000;8894738909374375872;4759769053262163701;00000000003;00000000003;00\n",
      "00000;00;2022-07-21T12:19:16.000+01:00;2022-07-21;77;249135;978;00000;8894738909374375872;4759769053262163701;00000000003;00000000003;00\n",
      "00000;00;2022-07-21;2022-07-21T12:19:16.000+01:00;77;249135;978;00000;8894738909374375872;4759769053262163701;00000000003;00000000003;00\n",
      "00000;00;2022-07-21;;77;249135;978;00000;8894738909374375872;4759769053262163701;00000000003;00000000003;00\n",
      "00000;00;2022-07-21;2022-07-;77;249135;978;00000;8894738909374375872;4759769053262163701;00000000003;00000000003;00\n",
      "00000;00;2022-07-21;2022-07-21;-1;249135;978;00000;8894738909374375872;4759769053262163701;00000000003;00000000003;00\n",
      "00000;00;2022-07-21;2022-07-21;;249135;978;00000;8894738909374375872;4759769053262163701;00000000003;00000000003;00\n",
      "00000;00;2022-07-21;2022-07-21;-1;249135;978;00000;8894738909374375872;4759769053262163701;00000000003;00000000003;00\n",
      "00000;00;2022-07-21;2022-07-21;77;;978;00000;8894738909374375872;4759769053262163701;00000000003;00000000003;00\n",
      "00000;00;2022-07-21;2022-07-21;77;-1;978;00000;8894738909374375872;4759769053262163701;00000000003;00000000003;00\n",
      "00000;00;2022-07-21;2022-07-21;77;249135;;00000;8894738909374375872;4759769053262163701;00000000003;00000000003;00\n",
      "00000;00;2022-07-21;2022-07-21;77;249135;EUR;00000;8894738909374375872;4759769053262163701;00000000003;00000000003;00\n",
      "00000;00;2022-07-21;2022-07-21;77;249135;978;;8894738909374375872;4759769053262163701;00000000003;00000000003;00\n",
      "00000;00;2022-07-21;2022-07-21;77;249135;978;00000;;4759769053262163701;00000000003;00000000003;00\n",
      "00000;00;2022-07-21;2022-07-21;77;249135;978;00000;8894738909374375872;;00000000003;00000000003;00\n",
      "00000;00;2022-07-21;2022-07-21;77;249135;978;00000;8894738909374375872;4759769053262163701;;00000000003;00\n",
      "00000;00;2022-07-21;2022-07-21;77;249135;978;00000;8894738909374375872;4759769053262163701;00000000003;00000000003;\n",
      "00000;00;2022-07-21;2022-07-21;77;249135;978;00000;8894738909374375872;4759769053262163701;00000000003;00000000003;0\n",
      "00000;00;2022-07-21;2022-07-21;77;249135;978;00000;8894738909374375872;4759769053262163701;00000000003;00000000003;000\n"})
  void shouldFailValidateTAE(String malformedAggregateRecord) throws IOException {
    Files.write(Path.of(tmpDirectory, blobNameTAEEmpty + ".decrypted"),
        malformedAggregateRecord.getBytes(), StandardOpenOption.APPEND);

    blobVerifierImpl.verify(fakeBlobTAEEmpty);
    assertEquals(Status.DECRYPTED, fakeBlobTAEEmpty.getStatus());
  }


  @ParameterizedTest
  @ValueSource(strings = {
      "",
      "\n",
      "00000;01;10;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07\n"
        + "\n"
        + "00000;01;10;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      ";01;10;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "0000;01;10;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "000000;01;10;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00001;;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00001;1;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00001;001;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00001;+1;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00002;01;;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00002;01;0;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00002;01;001;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00002;01;+1;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00003;01;08;;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00003;01;08;3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00003;01;08;ac3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00003;01;08;+3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00004;01;08;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00004;01;08;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00004;01;08;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00004;01;08;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000z;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00004;01;08;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00004;01;08;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06'T'12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00004;01;08;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+24:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00004;01;08;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T25:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00005;01;08;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00005;01;08;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;+193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00006;01;08;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00007;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;+27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00008;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00008;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;a;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00009;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00009;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;97;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00009;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;9781;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00010;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00010;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;+09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00011;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00011;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;+400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00012;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00012;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;+80205005;40236010;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00013;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00013;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;+4023601;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00013;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00013;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;4023601;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00013;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;402360100;4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00014;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00014;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00014;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;490000;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00014;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;+4900;RSSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00015;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;+SSMRA80A01H501U;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00015;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501V0;12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00016;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;+12345678901;01;E197169A09GQNYI34PN3QPA1SDM07",
      "00017;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;;E197169A09GQNYI34PN3QPA1SDM07",
      "00017;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;0;E197169A09GQNYI34PN3QPA1SDM07",
      "00017;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;001;E197169A09GQNYI34PN3QPA1SDM07",
      "00017;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;a;E197169A09GQNYI34PN3QPA1SDM07",
      "00017;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;+1;E197169A09GQNYI34PN3QPA1SDM07",
      "00017;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;+1;e197169a09GQNYI34PN3QPA1SDM07",
      "00017;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;+1;E1971",
      "00017;00;09;c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9;2020-08-06T12:19:16.000+01:00;193531782008954810291361325409791762715;324393315321635981285487364925433121593;27571141360967615190853606122155739169;877690;978;09509;400000080205;80205005;40236010;4900;RSSMRA80A01H501U;12345678901;+1;E197169A09GQNYI34PN3QPA1SDM07AAA"})
  void shouldFailValidateRTD(String malformedAggregateRecord) throws IOException {
    Files.write(Path.of(tmpDirectory, blobNameRTDEmpty + ".decrypted"),
        malformedAggregateRecord.getBytes(), StandardOpenOption.APPEND);

    blobVerifierImpl.verify(fakeBlobRTDEmpty);
    assertEquals(Status.DECRYPTED, fakeBlobRTDEmpty.getStatus());
  }

  @Test
  void shouldNotSplitMissingFile() {

    //Set the wrong directory for locating the decrypted fake blob
    fakeBlobRTD.setTargetDir("pippo");
    fakeBlobRTD.setStatus(Status.DECRYPTED);

    blobVerifierImpl.verify(fakeBlobRTD);
    assertEquals(Status.DECRYPTED, fakeBlobRTD.getStatus());
  }

}
