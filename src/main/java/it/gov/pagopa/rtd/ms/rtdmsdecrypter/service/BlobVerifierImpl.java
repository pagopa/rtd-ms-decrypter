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
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Application;
import java.time.LocalDate;
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

  private long numberOfDeserializeRecords;
  /**
   * Verify method, used to verify the validity of the
   * {@link BlobApplicationAware} records
   * decrypted.
   */
  public BlobApplicationAware verify(BlobApplicationAware blob) {
    log.info("START Verifying {}", blob.getBlob());

    FileReader fileReader;
    boolean isValid = true;
    numberOfDeserializeRecords = 0;
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
  
    // Enrich report
    if (blob.getApp() == Application.ADE && isValid) {
      deserialized.forEach(i -> gatheringMetadataAndCount(blob,i));
    } else {
      numberOfDeserializeRecords = deserialized.count();
    }

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

  private void logVerificationInformation(String blobName, Long deserializedSize,
      Integer violations) {
    log.info("END Verifying {} records: {} valid , {} malformed", blobName,
        deserializedSize, violations);
  }

  private void gatheringMetadataAndCount(BlobApplicationAware blob, DecryptedRecord decryptedRecord){
    AdeTransactionsAggregate tempAdeAgg = (AdeTransactionsAggregate) decryptedRecord;
      blob.getNumMerchant().add(tempAdeAgg.getMerchantId());
      if (tempAdeAgg.getOperationType().equals("00")){
        blob.setNumPositiveTrx(blob.getNumPositiveTrx()+tempAdeAgg.getNumTrx());
        blob.setTotalAmountPositiveTrx(blob.getTotalAmountPositiveTrx()+ tempAdeAgg.getTotalAmount());
      }else{
        blob.setNumCancelledTrx(blob.getNumCancelledTrx()+tempAdeAgg.getNumTrx());
        blob.setTotalAmountCancelledTrx(blob.getTotalAmountCancelledTrx()+tempAdeAgg.getTotalAmount());
      }
      if(blob.getMinAccountingDate().isAfter(LocalDate.parse(tempAdeAgg.getAccountingDate()))){
        blob.setMinAccountingDate(LocalDate.parse(tempAdeAgg.getAccountingDate()));
      }
      if(blob.getMaxAccountingDate().isBefore(LocalDate.parse(tempAdeAgg.getAccountingDate()))){
        blob.setMaxAccountingDate(LocalDate.parse(tempAdeAgg.getAccountingDate()));
      }
      numberOfDeserializeRecords++;
  }
}
