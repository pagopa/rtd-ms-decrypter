package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import static it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Status.VERIFIED;

import com.opencsv.bean.BeanVerifier;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.exceptions.CsvException;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.config.VerifierFactory;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.AdeTransactionsAggregate;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.DecryptedRecord;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.ReportMetaData;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Application;
import java.time.LocalDate;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

  @Value("${decrypt.skipChecksum}")
  private boolean skipChecksum;

  @Autowired
  private VerifierFactory verifierFactory;

  /**
   * Verify method, used to verify the validity of the
   * {@link BlobApplicationAware} records
   * decrypted.
   */
  public BlobApplicationAware verify(BlobApplicationAware blob) {
    log.info("START Verifying {}", blob.getBlob());

    FileReader fileReader;
    BufferedReader bufferedReader;
    boolean isValid = true;
    AtomicLong numberOfDeserializeRecords = new AtomicLong(0);
    String checkSum = "";
    try {
      fileReader = new FileReader(Path.of(blob.getTargetDir(), blob.getBlob()).toFile());
      bufferedReader = new BufferedReader(fileReader);
      if (skipChecksum) {
        checkSum = bufferedReader.readLine();
      }
    } catch (FileNotFoundException e) {
      log.error("Error reading file {}", blob.getBlob());
      return blob;
    } catch (IOException e){
      log.error("Error reading checksum {}", blob.getBlob());
      return blob;
    }

    BeanVerifier<? extends DecryptedRecord> verifier = verifierFactory.getVerifier(blob.getApp());
    Class<? extends DecryptedRecord> beanClass = verifierFactory.getBeanClass(blob.getApp());

    CsvToBeanBuilder<DecryptedRecord> builder = new CsvToBeanBuilder<DecryptedRecord>(bufferedReader)
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

  private void logVerificationInformation(String blobName, Long deserializedSize,
      Integer violations) {
    log.info("END Verifying {} records: {} valid , {} malformed", blobName,
        deserializedSize, violations);
  }

  private void gatheringMetadataAndCount(BlobApplicationAware blob, DecryptedRecord decryptedRecord, AtomicLong numberOfDeserializeRecords) {
    AdeTransactionsAggregate tempAdeAgg = (AdeTransactionsAggregate) decryptedRecord;
    ReportMetaData reportMetaData = blob.getOriginalBlob().getReportMetaData();
    reportMetaData.getMerchantList().add(tempAdeAgg.getMerchantId());
    if (tempAdeAgg.getOperationType().equals("00")) {
      reportMetaData.setNumPositiveTrx(reportMetaData.getNumPositiveTrx() + tempAdeAgg.getNumTrx());
      reportMetaData.setTotalAmountPositiveTrx(
          reportMetaData.getTotalAmountPositiveTrx() + tempAdeAgg.getTotalAmount());
    } else {
      reportMetaData
          .setNumCanceledTrx(reportMetaData.getNumCanceledTrx() + tempAdeAgg.getNumTrx());
      reportMetaData.setTotalAmountCanceledTrx(
          reportMetaData.getTotalAmountCanceledTrx() + tempAdeAgg.getTotalAmount());
    }
    if (reportMetaData.getMinAccountingDate().isAfter(LocalDate.parse(tempAdeAgg.getAccountingDate()))) {
      reportMetaData.setMinAccountingDate(LocalDate.parse(tempAdeAgg.getAccountingDate()));
    }
    if (reportMetaData.getMaxAccountingDate().isBefore(LocalDate.parse(tempAdeAgg.getAccountingDate()))) {
      reportMetaData.setMaxAccountingDate(LocalDate.parse(tempAdeAgg.getAccountingDate()));
    }
    numberOfDeserializeRecords.incrementAndGet();
  }
}
