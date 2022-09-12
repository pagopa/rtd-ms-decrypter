package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import static it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Status.VERIFIED;

import com.opencsv.bean.BeanVerifier;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.config.VerifierFactory;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.AdeTransactionsAggregate;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Application;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.DecryptedRecord;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.RtdTransaction;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Implementation of the {@link BlobVerifier} interface, used to verify the validity of the
 * {@link BlobApplicationAware} records extracted from the input file.
 */
@Service
@Setter
@Slf4j
public class BlobVerifierImpl implements BlobVerifier {

  @Value("${decrypt.skipChecksum}")
  private boolean skipChecksum;

  @Autowired
  private VerifierFactory verifierFactory;

  /**
   * Verify method, used to verify the validity of the {@link BlobApplicationAware} records
   * decrypted.
   */
  public BlobApplicationAware verify(BlobApplicationAware blob) {
    log.info("START VERIFYING {}", blob.getBlob());

    FileReader fileReader;

    boolean isValid = true;

    try {
      fileReader = new FileReader(Path.of(blob.getTargetDir(), blob.getBlob()).toFile());
    } catch (FileNotFoundException e) {
      log.error("Error reading file {}", blob.getBlob());

      return blob;
    }

    BeanVerifier<? extends DecryptedRecord> verifier = verifierFactory.getVerifier(blob.getApp());
    Class<? extends DecryptedRecord> beanClass = verifierFactory.getBeanClass(blob.getApp());

    CsvToBeanBuilder<DecryptedRecord> builder = new CsvToBeanBuilder<DecryptedRecord>(fileReader)
        .withType(beanClass)
        .withSeparator(';')
        .withVerifier((BeanVerifier<DecryptedRecord>) verifier)
        .withThrowExceptions(false);

    if (skipChecksum) {
      builder.withSkipLines(1);
    }

    CsvToBean<DecryptedRecord> csvToBean = builder.build();

    List<DecryptedRecord> deserialized = csvToBean.parse();
    List<CsvException> violations = csvToBean.getCapturedExceptions();

    if (!violations.isEmpty()) {
      for (CsvException e : violations) {
        log.error("Validation error at line " + e.getLineNumber() + " : " + e.getMessage());
      }
      isValid = false;
    } else if (deserialized.isEmpty()) {
      log.error("No records found in file {}", blob.getBlob());
      isValid = false;
    }

    try {
      serializeValidRecords(deserialized, blob.getTargetDir(), blob.getBlob());
    } catch (Exception e) {
      isValid = false;
    }

    if (isValid) {
      blob.setStatus(VERIFIED);
    } else {
      blob.localCleanup();
    }

    logVerificationInformation(blob.getBlob(), deserialized.size(), violations.size());
    return blob;
  }

  private void logVerificationInformation(String blobName, Integer deserializedSize,
      Integer violations) {
    log.info("END VERIFYING {} records: {} valid , {} malformed", blobName,
        deserializedSize, violations);
  }

  private void serializeValidRecords(List<DecryptedRecord> deserialized, String targetDir,
      String blobName)
      throws IOException, CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(
        Path.of(targetDir, blobName).toFile()))) {
      StatefulBeanToCsv<DecryptedRecord> beanToCsv = new StatefulBeanToCsvBuilder<DecryptedRecord>(
          writer)
          .withSeparator(';')
          .withApplyQuotesToAll(false)
          .build();
      beanToCsv.write(deserialized);
    } catch (Exception e) {
      log.error("Error writing to file {}", blobName);
      throw e;
    }
  }
}
