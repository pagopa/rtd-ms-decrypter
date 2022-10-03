package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import static it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Status.VERIFIED;

import com.opencsv.bean.BeanVerifier;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.exceptions.CsvException;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.config.VerifierFactory;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.DecryptedRecord;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
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
    log.info("START Verifying {}", blob.getBlob());

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

    Stream<DecryptedRecord> deserialized = csvToBean.stream();

    long numberOfDeserializeRecords = deserialized.count();
    List<CsvException> violations = csvToBean.getCapturedExceptions();

    if (!violations.isEmpty()) {
      for (CsvException e : violations) {
        log.error("Validation error at line " + e.getLineNumber() + " : " + e.getMessage());
      }
      isValid = false;
    } else if (numberOfDeserializeRecords == 0) {
      log.error("No records found in file {}", blob.getBlob());
      isValid = false;
    }

    if (isValid) {
      blob.setStatus(VERIFIED);
    } else {
      blob.localCleanup();
    }

    logVerificationInformation(blob.getBlob(), numberOfDeserializeRecords,
        violations.size());
    return blob;
  }

  private void logVerificationInformation(String blobName, Long deserializedSize,
      Integer violations) {
    log.info("END Verifying {} records: {} valid , {} malformed", blobName,
        deserializedSize, violations);
  }
}
