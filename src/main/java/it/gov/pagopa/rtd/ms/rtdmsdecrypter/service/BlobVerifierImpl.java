package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import static it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Status.VERIFIED;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.bean.BeanVerifier;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.exceptions.CsvException;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.config.VerifierFactory;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Application;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.ContractMethodAttributes;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.DecryptedRecord;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.WalletContract;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
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
    if (blob.getApp() == Application.WALLET) {
      blob.setStatus(VERIFIED);
      return blob;
    }

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
    }

    logVerificationInformation(blob.getBlob(), numberOfDeserializeRecords,
        violations.size());
    return blob;
  }

  public static WalletContract deserializeAndVerifyContract(ObjectMapper objectMapper,
      JsonParser jsonParser, int contractsCounter) throws IOException {

    WalletContract contract;

    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      Validator validator = factory.getValidator();

      contract = objectMapper.readValue(jsonParser, WalletContract.class);

      Set<ConstraintViolation<WalletContract>> contractViolations = validator.validate(
          contract);

      if (contractViolations.isEmpty()) {

        if ((contract.getAction().equals("CREATE") && contract.getImportOutcome().equals("KO"))
            || (contract.getAction().equals("DELETE") && contract.getImportOutcome()
            .equals("OK"))) {
          log.error("Conflicting couple (action, import_outcome) on contract {}",
              (contractsCounter + 1));
          return null;
        }

        ContractMethodAttributes currentContractMethodAttributes = contract.getMethodAttributes();

        if (currentContractMethodAttributes != null) {
          Set<ConstraintViolation<ContractMethodAttributes>> contractMethodAttributeViolations = validator.validate(
              currentContractMethodAttributes);
          if (!contractMethodAttributeViolations.isEmpty()) {
            log.error("Method attibutes of contract {} are not valid", (contractsCounter + 1));
            return null;
          }
        } else if (!contract.getAction().equals("DELETE")) {
          log.error("Method attibutes of contract {} are empty", (contractsCounter + 1));
          return null;
        }

      } else {
        for (ConstraintViolation<WalletContract> violation : contractViolations) {
          log.error("Validation error in contract " + (contractsCounter + 1) + " : "
              + violation.getMessage());
        }
        return null;

      }
    }
    return contract;
  }

  private void logVerificationInformation(String blobName, Long deserializedSize,
      Integer violations) {
    log.info("END Verifying {} records: {} valid , {} malformed", blobName,
        deserializedSize, violations);
  }
}
