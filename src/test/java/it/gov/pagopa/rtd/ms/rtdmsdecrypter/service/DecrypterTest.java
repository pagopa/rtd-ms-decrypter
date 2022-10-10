package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Iterator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ContextConfiguration(classes = {DecrypterImpl.class})
@TestPropertySource(value = {"classpath:application-nokafka.yml"}, inheritProperties = false)
@ExtendWith(OutputCaptureExtension.class)
class DecrypterTest {

  @Autowired
  DecrypterImpl decrypterImpl;

  @Value("${decrypt.resources.base.path}")
  String resources;

  @Value("${decrypt.resources.base.path}/tmp")
  String tmpDirectory;

  @Value("${decrypt.private.key.password}")
  String privateKeyPassword;

  String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad00000000000";
  String blobName = "CSTAR.99910.TRNLOG.20220228.103107.001.01.csv.pgp";
  BlobApplicationAware fakeBlob;

  //Partially mocked decrypter
  DecrypterImpl mockDecrypterImpl;

  @BeforeEach
  void setUp() throws IOException, PGPException, NoSuchProviderException {

    String sourceFileName = "CSTAR.99999.TRNLOG.20220419.121045.001.01.csv";

    // Read the publicKey for encrypting file
    FileInputStream publicKey = new FileInputStream(resources + "/certs/public.key");

    //Create the encrypted file
    File decryptedFile = Path.of(tmpDirectory, "encrypted.pgp").toFile();
    decryptedFile.getParentFile().mkdirs();
    decryptedFile.createNewFile();

    FileOutputStream encrypted = new FileOutputStream(tmpDirectory + "/encrypted.pgp");
    // encrypt with the same routine used by batch service
    this.encryptFile(encrypted, resources + "/" + sourceFileName, this.readPublicKey(publicKey),
        true, true);

    // Read the publicKey for encrypting blob
    FileInputStream publicKeyBlob = new FileInputStream(resources + "/certs/public.key");

    // encrypt with the same routine used by batch service
    FileOutputStream encryptedBlob = new FileOutputStream(
        Path.of(tmpDirectory, blobName).toString());
    this.encryptFile(encryptedBlob, Path.of(resources, sourceFileName).toString(),
        this.readPublicKey(publicKeyBlob), false, true);

    //Instantiate fake blob
    fakeBlob = new BlobApplicationAware(
        "/blobServices/default/containers/" + container + "/blobs/" + blobName);
    fakeBlob.setTargetDir(tmpDirectory);
    fakeBlob.setStatus(BlobApplicationAware.Status.DOWNLOADED);

    mockDecrypterImpl = mock(DecrypterImpl.class);
  }

  @AfterEach
  void cleanTmpFiles() throws IOException {
    FileUtils.deleteDirectory(Path.of(tmpDirectory).toFile());
  }


  @Test
  void shouldDecodeBase64File() throws IOException {
    // After construction, Decrypter method readKey is called, so the key is in
    // private attribute
    assertEquals(Files.readString(Path.of(resources, "certs/private.key")),
        decrypterImpl.getPrivateKey());
  }

  @Test
  void shouldDecryptFile(CapturedOutput output)
      throws IOException, PGPException {

    // decrypt and compare
    FileInputStream myEncrypted = new FileInputStream(tmpDirectory + "/encrypted.pgp");
    FileOutputStream myClearText = new FileOutputStream(tmpDirectory + "/file.pgp.csv.decrypted");

    decrypterImpl.decryptFile(myEncrypted, myClearText, "encrypted.pgp");

    myClearText.close();

    assertTrue(IOUtils.contentEquals(
        new BufferedReader(new FileReader(Path.of(resources,
            "/CSTAR.99999.TRNLOG.20220419.121045.001.01.csv").toFile())),
        new BufferedReader(
            new FileReader(Path.of(tmpDirectory, "/file.pgp.csv.decrypted").toFile()))));

    //Ensures, through log info, that all file decrypting steps are done
    assertThat(output.getOut(), containsString("Copying decrypted stream:"));
    assertThat(output.getOut(), containsString("Closing:"));
    assertThat(output.getOut(), containsString("Closing clear stream:"));

  }

  @Test
  void shouldThrowIoExceptionFromDecryptingMalformedPgpFile()
      throws IOException {

    // Try to decrypt a malformed encrypted file
    FileInputStream myMalformedEncrypted = new FileInputStream(
        resources + "/malformedEncrypted.pgp");
    FileOutputStream myClearText = new FileOutputStream(tmpDirectory + "/malformedFile.decrypted");
    assertThrows(IOException.class, () -> {
      decrypterImpl.decryptFile(myMalformedEncrypted, myClearText, "malformedEncrypted.pgp");
    });

    myClearText.close();

  }

  @Test
  void shouldThrowIllegalArgumentExceptionFromDecryptingEncryptedNoData(CapturedOutput output)
      throws IOException, NoSuchProviderException, PGPException {

    // Read the publicKey
    FileInputStream publicKey = new FileInputStream(resources + "/certs/public.key");

    // Encrypt an empty file
    FileOutputStream myEmpty = new FileOutputStream(
        tmpDirectory + "/emptyFile");
    FileOutputStream myEmptyEncryptedOutput = new FileOutputStream(
        tmpDirectory + "/emptyFile.pgp");
    this.encryptFile(myEmptyEncryptedOutput, tmpDirectory + "/emptyFile",
        this.readPublicKey(publicKey),
        true, true);
    myEmpty.close();
    myEmptyEncryptedOutput.close();

    FileInputStream myEmptyEncryptedInput = new FileInputStream(tmpDirectory + "/emptyFile.pgp");
    FileOutputStream myClearText = new FileOutputStream(tmpDirectory + "/emptyFile.decrypted");

    // Try to decrypt the empty file, resulting in an IllegalArgumentException
    assertThrows(IllegalArgumentException.class, () -> {
      decrypterImpl.decryptFile(myEmptyEncryptedInput, myClearText, blobName);
    });
    myEmptyEncryptedInput.close();
    myClearText.close();

    //Ensures, through log info, that the correct decrypting steps are done
    assertThat(output.getOut(), containsString("Copying decrypted stream:"));
    assertThat(output.getOut(), containsString("Closing:"));
    assertThat(output.getOut(), containsString("Closing clear stream:"));

  }

  @Test
  void shouldDecrypt(CapturedOutput output)
      throws IOException {

    // decrypt and compare

    decrypterImpl.decrypt(fakeBlob);

    assertTrue(IOUtils.contentEquals(
        new BufferedReader(new FileReader(Path.of(resources,
            "/CSTAR.99999.TRNLOG.20220419.121045.001.01.csv").toFile())),
        new BufferedReader(
            new FileReader(Path.of(tmpDirectory, fakeBlob.getBlob() + ".decrypted").toFile()))
    ));

    //Check if the local blob and the decrypted one aren't cleaned up
    assertTrue(Files.exists(Path.of(tmpDirectory, blobName)) && Files.exists(
        Path.of(tmpDirectory, blobName + ".decrypted")));

    //Ensures, through log info, that all decrypting steps are done
    assertThat(output.getOut(), containsString("Copying decrypted stream:"));
    assertThat(output.getOut(), containsString("Closing:"));
    assertThat(output.getOut(), containsString("Closing clear stream:"));
    assertThat(output.getOut(), containsString("Blob decrypted:"));

  }

  @Test
  void shouldWarnNoData(CapturedOutput output)
      throws IOException, PGPException {

    //Mock decrypter behaviour
    when(mockDecrypterImpl.decrypt(any(BlobApplicationAware.class))).thenCallRealMethod();
    doThrow(new IllegalArgumentException("No data found in decrypted file")).when(
            mockDecrypterImpl)
        .decryptFile(any(), any(), any());

    mockDecrypterImpl.decrypt(fakeBlob);

    assertThat(output.getOut(), containsString("No data found in decrypted file"));

    //Check if the local blob is cleaned up (given that it's empty)
    assertFalse(Files.exists(Path.of(tmpDirectory, blobName)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Secret key for message not found.",
      "Encrypted message contains a signed message - not literal data.",
      "Message is not a simple encrypted file - type unknown."})
  void shouldFailDecryptPGPException(String error, CapturedOutput output)
      throws IOException, PGPException {

    //Mock decrypter behaviour
    when(mockDecrypterImpl.decrypt(any(BlobApplicationAware.class))).thenCallRealMethod();
    doThrow(new PGPException(error)).when(
        mockDecrypterImpl).decryptFile(any(), any(), any());

    mockDecrypterImpl.decrypt(fakeBlob);

    assertThat(output.getOut(), containsString(error));

    //Check if the local blob is cleaned up
    assertFalse(Files.exists(Path.of(tmpDirectory, blobName)));
  }

  @Test
  void shouldNotDecryptIOException(CapturedOutput output)
      throws IOException, PGPException {

    //Mock decrypter behaviour
    when(mockDecrypterImpl.decrypt(any(BlobApplicationAware.class))).thenCallRealMethod();
    doThrow(
        new IOException("invalid armor")).when(
        mockDecrypterImpl).decryptFile(any(), any(), any());

    mockDecrypterImpl.decrypt(fakeBlob);

    assertThat(output.getOut(),
        containsString("invalid armor"));

    //Check if the local blob is cleaned up
    assertFalse(Files.exists(Path.of(tmpDirectory, blobName)));

  }

  // This routine should be factored out in a common module
  // https://github.com/pagopa/rtd-ms-transaction-filter/blob/76ef81bd58be8c9a9d417735c87ad1c08360a091/api/batch/src/main/java/it/gov/pagopa/rtd/transaction_filter/batch/encryption/EncryptUtil.java#L194
  private void encryptFile(
      OutputStream out,
      String fileName,
      PGPPublicKey encKey,
      boolean armor,
      boolean withIntegrityCheck)
      throws IOException {

    Security.addProvider(new BouncyCastleProvider());

    if (armor) {
      out = new ArmoredOutputStream(out);
    }

    try {
      PGPEncryptedDataGenerator cPk = new PGPEncryptedDataGenerator(
          new JcePGPDataEncryptorBuilder(PGPEncryptedData.CAST5)
              .setWithIntegrityPacket(withIntegrityCheck)
              .setSecureRandom(new SecureRandom()).setProvider("BC"));

      cPk.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(encKey).setProvider("BC"));

      OutputStream cOut = cPk.open(out, new byte[1 << 16]);

      PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(
          PGPCompressedData.ZIP);

      PGPUtil.writeFileToLiteralData(
          comData.open(cOut), PGPLiteralData.BINARY, new File(fileName), new byte[1 << 16]);

      comData.close();

      cOut.close();

      if (armor) {
        out.close();
      }
    } catch (PGPException e) {
      System.err.println(e);
      if (e.getUnderlyingException() != null) {
        e.getUnderlyingException().printStackTrace();
      }

    }

  }

  private PGPPublicKey readPublicKey(InputStream input) throws IOException, PGPException {
    PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(
        PGPUtil.getDecoderStream(input), new JcaKeyFingerprintCalculator());

    Iterator<PGPPublicKeyRing> keyRingIter = pgpPub.getKeyRings();
    while (keyRingIter.hasNext()) {
      PGPPublicKeyRing keyRing = (PGPPublicKeyRing) keyRingIter.next();

      Iterator<PGPPublicKey> keyIter = keyRing.getPublicKeys();
      while (keyIter.hasNext()) {
        PGPPublicKey key = (PGPPublicKey) keyIter.next();

        if (key.isEncryptionKey()) {
          return key;
        }
      }
    }

    throw new IllegalArgumentException("Can't find encryption key in key ring.");
  }

}
