package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
@ContextConfiguration(classes = {DecrypterImpl.class})
@TestPropertySource(value = {"classpath:application-nokafka.yml"}, inheritProperties = false)
@ExtendWith(OutputCaptureExtension.class)
class DecrypterTest {

  @Autowired
  DecrypterImpl decrypterImpl;

  @Value("${decrypt.resources.base.path}")
  String resources;

  @Value("${decrypt.private.key.password}")
  String privateKeyPassword;

  String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad00000000000";
  String blobName = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp";
  BlobApplicationAware fakeBlob = new BlobApplicationAware(
      "/blobServices/default/containers/" + container + "/blobs/" + blobName);

  @Test
  void shouldDecodeBase64File() throws IOException {
    // After construction, Decrypter method readKey is called, so the key is in
    // private attribute
    assertEquals(Files.readString(Path.of(resources, "certs/private.key")),
        decrypterImpl.getPrivateKey());
  }

  @Test
  void shouldDecryptFile() throws IOException, NoSuchProviderException, PGPException {

    // generate file
    String sourceFileName = "cleartext.csv";

    // Read the publicKey
    FileInputStream publicKey = new FileInputStream(resources + "/certs/public.key");

    // encrypt with the same routine used by batch service
    FileOutputStream encrypted = new FileOutputStream(resources + "/encrypted.pgp");
    this.encryptFile(encrypted, resources + "/" + sourceFileName, this.readPublicKey(publicKey),
        true, true);

    // decrypt and compare
    FileInputStream myEncrypted = new FileInputStream(resources + "/encrypted.pgp");
    FileOutputStream myClearText = new FileOutputStream(resources + "/file.pgp.csv.decrypted");

    decrypterImpl.decryptFile(myEncrypted, myClearText);
    myClearText.close();

    assertTrue(IOUtils.contentEquals(
        new BufferedReader(new FileReader(Path.of(resources, "/cleartext.csv").toFile())),
        new BufferedReader(
            new FileReader(Path.of(resources, "/file.pgp.csv.decrypted").toFile()))));
  }

  @Test
  void shouldThrowIOExceptionFromMalformedPGPFile()
      throws IOException, NoSuchProviderException, PGPException {

    // Try to decrypt a malformed encrypted file
    FileInputStream myMalformedEncrypted = new FileInputStream(
        resources + "/malformedEncrypted.pgp");
    FileOutputStream myClearText = new FileOutputStream(resources + "/file.pgp.csv.decrypted");
    assertThrows(IOException.class, () -> {
      decrypterImpl.decryptFile(myMalformedEncrypted, myClearText);
    });

    myClearText.close();
  }

  @Test
  void shouldDecrypt() throws IOException, NoSuchProviderException, PGPException {

    // generate file
    String sourceFileName = "cleartext.csv";

    // Read the publicKey
    FileInputStream publicKey = new FileInputStream(
        Path.of(resources, "/certs/public.key").toString());

    // encrypt with the same routine used by batch service
    FileOutputStream encrypted = new FileOutputStream(Path.of(resources, blobName).toString());
    this.encryptFile(encrypted, Path.of(resources, sourceFileName).toString(),
        this.readPublicKey(publicKey), false, true);

    // decrypt and compare
    fakeBlob.setTargetDir(resources);
    fakeBlob.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    decrypterImpl.decrypt(fakeBlob);

    assertTrue(IOUtils.contentEquals(
        new BufferedReader(new FileReader(Path.of(resources, "/cleartext.csv").toFile())),
        new BufferedReader(
            new FileReader(Path.of(resources, fakeBlob.getBlob() + ".decrypted").toFile()))
    ));
  }

  @Test
  void shouldWarnNoData(CapturedOutput output)
      throws IOException, NoSuchProviderException, PGPException {

    // generate file
    String sourceFileName = "cleartext.csv";

    // Read the publicKey
    FileInputStream publicKey = new FileInputStream(
        Path.of(resources, "/certs/public.key").toString());

    // encrypt with the same routine used by batch service
    FileOutputStream encrypted = new FileOutputStream(Path.of(resources, blobName).toString());
    this.encryptFile(encrypted, Path.of(resources, sourceFileName).toString(),
        this.readPublicKey(publicKey), false, true);

    //Partially mocked decrypter
    DecrypterImpl mockDecrypterImpl = mock(DecrypterImpl.class);

    when(mockDecrypterImpl.decrypt(any(BlobApplicationAware.class))).thenCallRealMethod();
    doThrow(new IllegalArgumentException("Can't extract data from encrypted file")).when(
            mockDecrypterImpl)
        .decryptFile(any(), any());

    fakeBlob.setTargetDir(resources);
    fakeBlob.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    mockDecrypterImpl.decrypt(fakeBlob);

    assertThat(output.getOut(), containsString("Can't extract data from encrypted file"));
  }

  @Test
  void shouldFailDecryptNoSecretKey(CapturedOutput output)
      throws IOException, NoSuchProviderException, PGPException {

    //Partially mocked decrypter
    DecrypterImpl mockDecrypterImpl = mock(DecrypterImpl.class);

    when(mockDecrypterImpl.decrypt(any(BlobApplicationAware.class))).thenCallRealMethod();
    doThrow(new PGPException("Secret key for message not found.")).when(
        mockDecrypterImpl).decryptFile(any(), any());

    fakeBlob.setTargetDir(resources);
    fakeBlob.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    mockDecrypterImpl.decrypt(fakeBlob);

    assertThat(output.getOut(), containsString("Secret key for message not found."));
  }

  @Test
  void shouldNotDecryptNoLiteralData(CapturedOutput output)
      throws IOException, NoSuchProviderException, PGPException {

    String sourceFileName = "cleartext.csv";

    // Read the publicKey
    FileInputStream publicKey = new FileInputStream(
        Path.of(resources, "/certs/public.key").toString());

    // encrypt with the same routine used by batch service
    FileOutputStream encrypted = new FileOutputStream(Path.of(resources, blobName).toString());
    this.encryptFile(encrypted, Path.of(resources, sourceFileName).toString(),
        this.readPublicKey(publicKey), false, true);

    //Partially mocked decrypter
    DecrypterImpl mockDecrypterImpl = mock(DecrypterImpl.class);

    when(mockDecrypterImpl.decrypt(any(BlobApplicationAware.class))).thenCallRealMethod();
    doThrow(
        new PGPException("Encrypted message contains a signed message - not literal data.")).when(
        mockDecrypterImpl).decryptFile(any(), any());

    fakeBlob.setTargetDir(resources);
    fakeBlob.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    mockDecrypterImpl.decrypt(fakeBlob);

    assertThat(output.getOut(),
        containsString("Encrypted message contains a signed message - not literal data."));
  }

  @Test
  void shouldNotDecryptTypeUnknown(CapturedOutput output)
      throws IOException, NoSuchProviderException, PGPException {

    // generate file
    String sourceFileName = "cleartext.csv";

    // Read the publicKey
    FileInputStream publicKey = new FileInputStream(
        Path.of(resources, "/certs/public.key").toString());

    // encrypt with the same routine used by batch service
    FileOutputStream encrypted = new FileOutputStream(Path.of(resources, blobName).toString());
    this.encryptFile(encrypted, Path.of(resources, sourceFileName).toString(),
        this.readPublicKey(publicKey), false, true);

    //Partially mocked decrypter
    DecrypterImpl mockDecrypterImpl = mock(DecrypterImpl.class);

    when(mockDecrypterImpl.decrypt(any(BlobApplicationAware.class))).thenCallRealMethod();
    doThrow(new PGPException("Message is not a simple encrypted file - type unknown.")).when(
        mockDecrypterImpl).decryptFile(any(), any());

    fakeBlob.setTargetDir(resources);
    fakeBlob.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    mockDecrypterImpl.decrypt(fakeBlob);

    assertThat(output.getOut(),
        containsString("Message is not a simple encrypted file - type unknown."));
  }

  // This routine should be factored out in a common module
  // https://github.com/pagopa/rtd-ms-transaction-filter/blob/76ef81bd58be8c9a9d417735c87ad1c08360a091/api/batch/src/main/java/it/gov/pagopa/rtd/transaction_filter/batch/encryption/EncryptUtil.java#L194
  private void encryptFile(
      OutputStream out,
      String fileName,
      PGPPublicKey encKey,
      boolean armor,
      boolean withIntegrityCheck)
      throws IOException, NoSuchProviderException {

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
