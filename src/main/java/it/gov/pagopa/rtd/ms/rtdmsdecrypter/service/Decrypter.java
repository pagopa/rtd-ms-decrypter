package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.util.Base64;
import java.util.Iterator;

import javax.annotation.PostConstruct;

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

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;

import org.apache.commons.io.IOUtils;

@Service
@Slf4j
@Getter
public class Decrypter implements IDecrypter {

  @Value("${decrypt.private.key.path}")
  private String privateKeyPath;

  @Value("${decrypt.private.key.password}")
  private String privateKeyPassword;

  private String privateKey;


  @PostConstruct
  private void readKey() throws IOException {
    this.privateKey = new String(Base64.getMimeDecoder().decode(Files.readString(Path.of(this.privateKeyPath))));
  }

  public BlobApplicationAware decrypt(BlobApplicationAware blob) {


    try (
      FileInputStream encrypted = new FileInputStream(Path.of(blob.getTargetDir(), blob.getBlob()).toString());
      FileOutputStream decrypted = new FileOutputStream(Path.of(blob.getTargetDir(), blob.getBlob() + ".decrypted").toString() );
    ) {
      
      this.decryptFile(encrypted, decrypted);
    }
    catch (Exception ex) {
      log.error("Cannot Decrypt. {}", ex.getMessage());
    }
    blob.setStatus(BlobApplicationAware.Status.DECRYPTED);
    return blob;
  }

  @SneakyThrows
  protected void decryptFile(InputStream input, OutputStream output) {

    InputStream keyInput = IOUtils.toInputStream(this.privateKey, StandardCharsets.UTF_8);
    char[] passwd = this.privateKeyPassword.toCharArray();

    Security.addProvider(new BouncyCastleProvider());
    input = PGPUtil.getDecoderStream(input);
    InputStream unencrypted = null;
    InputStream clear = null;

    try {
      JcaPGPObjectFactory pgpF = new JcaPGPObjectFactory(input);
      PGPEncryptedDataList encrypted;

      Object object = pgpF.nextObject();
      //
      // the first object might be a PGP marker packet.
      //
      if (object instanceof PGPEncryptedDataList) {
        encrypted = (PGPEncryptedDataList) object;
      } else {
        encrypted = (PGPEncryptedDataList) pgpF.nextObject();
      }

      //
      // find the secret key
      //
      Iterator<PGPEncryptedData> it = encrypted.getEncryptedDataObjects();
      PGPPrivateKey sKey = null;
      PGPPublicKeyEncryptedData pbe = null;
      PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(
          PGPUtil.getDecoderStream(keyInput), new JcaKeyFingerprintCalculator());

      while (sKey == null && it.hasNext()) {
        pbe = (PGPPublicKeyEncryptedData) it.next();

        sKey = findSecretKey(pgpSec, pbe.getKeyID(), passwd);
      }

      if (sKey == null) {
        throw new IllegalArgumentException("secret key for message not found.");
      }

      clear = pbe.getDataStream(new JcePublicKeyDataDecryptorFactoryBuilder()
          .setProvider("BC").build(sKey));

      JcaPGPObjectFactory plainFact = new JcaPGPObjectFactory(clear);

      Object message = plainFact.nextObject();

      if (message instanceof PGPCompressedData) {
        PGPCompressedData cData = (PGPCompressedData) message;
        JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(cData.getDataStream());

        message = pgpFact.nextObject();
      }

      if (message instanceof PGPLiteralData) {
        PGPLiteralData ld = (PGPLiteralData) message;

        unencrypted = ld.getInputStream();
        IOUtils.copy(unencrypted, output);

      } else if (message instanceof PGPOnePassSignatureList) {
        throw new PGPException("encrypted message contains a signed message - not literal data.");
      } else {
        throw new PGPException("message is not a simple encrypted file - type unknown.");
      }

    } catch (PGPException e) {
      log.error("PGPException {}", e.getMessage());
      throw e;

    } finally {
      keyInput.close();
      if (unencrypted != null) {
        unencrypted.close();
      }
      if (clear != null) {
        clear.close();
      }
    }

  }

  @Nullable
  private PGPPrivateKey findSecretKey(PGPSecretKeyRingCollection pgpSec, long keyID, char[] pass)
      throws PGPException {
    PGPSecretKey pgpSecKey = pgpSec.getSecretKey(keyID);

    if (pgpSecKey == null) {
      return null;
    }

    return pgpSecKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(pass));
  }

}
