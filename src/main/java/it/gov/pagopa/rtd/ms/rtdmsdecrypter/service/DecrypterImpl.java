package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.utils.LargeFileUtils;
import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.Security;
import java.util.Base64;
import java.util.Iterator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * Class implementing PGP decryption logic.
 */
@Service
@Slf4j
@Getter
public class DecrypterImpl implements Decrypter {

  @Value("${decrypt.private.key.password}")
  private String privateKeyPassword;

  @Value("${decrypt.private.key.base64}")
  private String privateKeyBase64;

  private String privateKey;

  @PostConstruct
  private void readKey() {
    this.privateKey = new String(Base64.getMimeDecoder().decode(this.privateKeyBase64));
  }

  /**
   * Constructor.
   *
   * @param blob a blob containing a PGP encrypted transactions file.
   * @return an unencrypted blob
   */
  public BlobApplicationAware decrypt(BlobApplicationAware blob) {
    log.info("Start decrypt blob: {}", blob.getBlob());

    boolean decryptFailed = false;
    try (
        FileInputStream encrypted = new FileInputStream(
            Path.of(blob.getTargetDir(), blob.getBlob()).toFile());
        FileOutputStream decrypted = new FileOutputStream(
            Path.of(blob.getTargetDir(), blob.getBlob() + ".decrypted").toFile())
    ) {

      this.decryptFile(encrypted, decrypted, blob.getBlob());
      blob.setStatus(BlobApplicationAware.Status.DECRYPTED);
      log.info("Blob decrypted: {}", blob.getBlob());

    } catch (IllegalArgumentException e) {
      log.warn("{}: {}", e.getMessage(), blob.getBlob());
      decryptFailed = true;
    } catch (Exception e) {
      log.error("Cannot decrypt {}: {} {}", blob.getBlob(), e.getMessage(), e.getCause());
      decryptFailed = true;
    }

    // If the decrypt failed this call to localCleanup ensures that no local files are left
    // On a non-failing scenario the files are cleaned at the end of the handler
    if (decryptFailed) {
      blob.localCleanup();
    }

    return blob;
  }

  protected void decryptFile(InputStream input, OutputStream output, String blobName)
      throws IOException, PGPException {
    log.info("Getting private key");
    InputStream keyInput = IOUtils.toInputStream(this.privateKey, StandardCharsets.UTF_8);
    char[] passwd = this.privateKeyPassword.toCharArray();

    Security.addProvider(new BouncyCastleProvider());
    input = PGPUtil.getDecoderStream(input);
    InputStream unencrypted = null;
    InputStream clear = null;

    try {
      JcaPGPObjectFactory pgpF = new JcaPGPObjectFactory(input);
      PGPEncryptedDataList encrypted;
      log.info("Try getting object from pgp file");

      Object object = pgpF.nextObject();
      // The first object might be a PGP marker packet.
      if (object instanceof PGPEncryptedDataList pgpencrypteddatalist) {
        encrypted = pgpencrypteddatalist;
      } else {
        encrypted = (PGPEncryptedDataList) pgpF.nextObject();
      }

      log.info("Iterating over objects");
      // Find the secret key
      Iterator<PGPEncryptedData> it = encrypted.getEncryptedDataObjects();
      PGPPrivateKey secretKey = null;
      PGPPublicKeyEncryptedData pbe = null;
      PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(
          PGPUtil.getDecoderStream(keyInput), new JcaKeyFingerprintCalculator());

      while (secretKey == null && it.hasNext()) {
        pbe = (PGPPublicKeyEncryptedData) it.next();
        secretKey = findSecretKey(pgpSec, pbe.getKeyID(), passwd);
      }

      if (secretKey == null) {
        throw new PGPException("Secret key for message not found.");
      }

      log.info("Secret key has been found");

      clear = pbe.getDataStream(new JcePublicKeyDataDecryptorFactoryBuilder()
          .setProvider("BC").build(secretKey));

      JcaPGPObjectFactory plainFact = new JcaPGPObjectFactory(clear);

      log.info("Getting message");

      Object message = plainFact.nextObject();

      if (message instanceof PGPCompressedData pgpCompressedData) {
        PGPCompressedData data = pgpCompressedData;
        JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(data.getDataStream());

        message = pgpFact.nextObject();
      }

      if (message instanceof PGPLiteralData pgpLiteralData) {
        PGPLiteralData ld = pgpLiteralData;

        unencrypted = ld.getInputStream();

        log.info("Copying decrypted stream: {}", blobName);
        if (LargeFileUtils.copy(unencrypted, output) <= 0) {
          throw new IllegalArgumentException("No data found in decrypted file");
        }

      } else if (message instanceof PGPOnePassSignatureList) {
        throw new PGPException("Encrypted message contains a signed message - not literal data.");
      } else {
        throw new PGPException("Message is not a simple encrypted file - type unknown.");
      }

    } finally {
      keyInput.close();
      if (unencrypted != null) {
        log.info("Closing: {}", blobName + ".decrypted");
        unencrypted.close();
      }
      if (clear != null) {
        log.info("Closing clear stream: {}", blobName);
        clear.close();
      }
    }

  }

  @Nullable
  private PGPPrivateKey findSecretKey(PGPSecretKeyRingCollection pgpSec, long keyId, char[] pass)
      throws PGPException {
    PGPSecretKey pgpSecKey = pgpSec.getSecretKey(keyId);

    if (pgpSecKey == null) {
      return null;
    }

    return pgpSecKey.extractPrivateKey(
        new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(pass));
  }

}
