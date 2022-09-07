package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import static it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Status.VERIFIED;

import com.opencsv.bean.BeanVerifier;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.exceptions.CsvException;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.AdeTransactionsAggregate;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Application;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.RtdTransaction;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.List;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Setter
@Slf4j
public class BlobVerifierImpl implements BlobVerifier {

  public BlobApplicationAware verify(BlobApplicationAware blob) {
    log.info("Start evaluating blob {} from {}", blob.getBlob(), blob.getContainer());

    BeanVerifier verifier = new RtdTransactionsVerifier();
    Class beanClass = RtdTransaction.class;

    if (Application.ADE.equals(blob.getApp())) {
      verifier = new AdeAggregatesVerifier();
      beanClass = AdeTransactionsAggregate.class;
    }

    try {
      CsvToBean b = new CsvToBeanBuilder(new FileReader(
          Path.of(blob.getTargetDir(), blob.getBlob()).toFile()))
          .withType(beanClass)
          .withSeparator(';')
          .withVerifier(verifier)
          // skip the checksum line
          .withSkipLines(1)
          .withThrowExceptions(false)
          .build();

      // The parsing is necessary to trigger the verification of the beans
      List deserialized = b.parse();
      List<CsvException> exceptions = b.getCapturedExceptions();

      if (deserialized.isEmpty()) {
        if (!exceptions.isEmpty()) {
          for (CsvException e : exceptions) {
            log.error("Validation error at line " + e.getLineNumber() + " : " + e.getMessage());
          }
        } else {
          log.error("No valid records found in blob {}", blob.getBlob());
        }
        return blob;
      }

      if (!exceptions.isEmpty()) {
        for (CsvException e : exceptions) {
          log.error("Validation error at line " + e.getLineNumber() + " : " + e.getMessage());
        }
        return blob;
      }

      log.info("Successful validation of blob:{}", blob.getBlob());
      blob.setStatus(VERIFIED);
      return blob;

    } catch (FileNotFoundException e) {
      return blob;
    }
  }
}
