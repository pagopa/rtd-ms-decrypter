package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import static it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Status.VERIFIED;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.bean.BeanVerifier;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.exceptions.CsvException;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.config.VerifierFactory;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.AdeTransactionsAggregate;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Application;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.ContractMethodAttributes;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.DecryptedRecord;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.ReportMetaData;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Application;
import java.time.LocalDate;

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

import java.util.concurrent.atomic.AtomicLong;

import java.util.Set;

import java.util.stream.Stream;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of the {@link BlobVerifier} interface, used to verify the
 * validity of the
 * {@link BlobApplicationAware} records extracted from the input file.
 */
@Service
@Setter
@Slf4j
public class BlobVerifierImpl implements BlobVerifier {


  @Autowired
  private VerifierFactory verifierFactory;

  /**
   * Verify method, used to verify the validity of the
   * {@link BlobApplicationAware} records
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
    AtomicLong numberOfDeserializeRecords = new AtomicLong(0);
    String checkSum = "";
    try {
      fileReader = new FileReader(Path.of(blob.getTargetDir(), blob.getBlob()).toFile());
    } catch (FileNotFoundException e) {
      log.error("Error reading file {}", blob.getBlob());
      return blob;
    } catch (IOException e) {
      log.error("Error reading checksum {}", blob.getBlob());
      return blob;
    }

    BeanVerifier<? extends DecryptedRecord> verifier = verifierFactory.getVerifier(blob.getApp());
    Class<? extends DecryptedRecord> beanClass = verifierFactory.getBeanClass(blob.getApp());

    CsvToBeanBuilder<DecryptedRecord> builder = new CsvToBeanBuilder<DecryptedRecord>(fileReader)
        .withType(beanClass)
        .withSeparator(';')
        .withVerifier((BeanVerifier<DecryptedRecord>) verifier)
        .withThrowExceptions(false);

    CsvToBean<DecryptedRecord> csvToBean = builder.build();

    Stream<DecryptedRecord> deserialized = csvToBean.stream();

    // Enrich report
    if (blob.getApp() == Application.ADE && isValid) {
      deserialized.forEach(i -> gatheringMetadataAndCount(blob, i, numberOfDeserializeRecords));
      blob.getOriginalBlob().getReportMetaData().setCheckSum(checkSum);
    } else {
      numberOfDeserializeRecords.set(deserialized.count());
    }

    List<CsvException> violations = csvToBean.getCapturedExceptions();

    if (!violations.isEmpty()) {
      for (CsvException e : violations) {
        log.error("Validation error at line " + e.getLineNumber() + " : " + e.getMessage());
      }
      isValid = false;
    } else if (numberOfDeserializeRecords.get() == 0) {
      log.error("No records found in file {}", blob.getBlob());
      isValid = false;
    }

    if (isValid) {
      blob.setStatus(VERIFIED);
    }
    logVerificationInformation(blob.getBlob(), numberOfDeserializeRecords.get(),
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

        if (contract.getAction().equals("CREATE") && !contract.getImportOutcome().equals("OK")) {
          log.error(
              "Validation error on contract {}: conflicting action CREATE and import_outcome not OK",
              (contractsCounter + 1));
          return null;
        }

        if (contract.getImportOutcome().equals("KO") && contract.getReasonMessage() == null) {
          log.error("Validation error on contract {}: import outcome KO and no reason message",
              (contractsCounter + 1));
          return null;
        }

        ContractMethodAttributes currentContractMethodAttributes = contract.getMethodAttributes();

        if (currentContractMethodAttributes != null) {
          Set<ConstraintViolation<ContractMethodAttributes>> contractMethodAttributeViolations = validator.validate(
              currentContractMethodAttributes);
          if (!contractMethodAttributeViolations.isEmpty()) {
            log.error("Validation error on contract {}: method attributes are not valid",
                (contractsCounter + 1));
            for (ConstraintViolation<ContractMethodAttributes> violation : contractMethodAttributeViolations) {
              log.error("{} {}", violation.getPropertyPath(), violation.getMessage());
            }
            return null;
          }
        } else if (!contract.getAction().equals("DELETE")) {
          log.error("Method attributes of contract {} are empty", (contractsCounter + 1));
          return null;
        }

      } else {
        log.error("Validation error in contract {}: fields are not valid", (contractsCounter + 1));
        for (ConstraintViolation<WalletContract> violation : contractViolations) {
          log.error("{}", violation.getMessage());
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

  private void gatheringMetadataAndCount(BlobApplicationAware blob, DecryptedRecord decryptedRecord,
      AtomicLong numberOfDeserializeRecords) {
    AdeTransactionsAggregate tempAdeAgg = (AdeTransactionsAggregate) decryptedRecord;
    ReportMetaData reportMetaData = blob.getOriginalBlob().getReportMetaData();
    reportMetaData.getMerchantList().add(tempAdeAgg.getMerchantId());
    reportMetaData.increaseTrx(tempAdeAgg.getOperationType(), tempAdeAgg.getNumTrx());
    reportMetaData.increaseTotalAmountTrx(tempAdeAgg.getOperationType(), tempAdeAgg.getTotalAmount());
    reportMetaData.updateAccountingDate(LocalDate.parse(tempAdeAgg.getAccountingDate()));
    numberOfDeserializeRecords.incrementAndGet();
  }
}
