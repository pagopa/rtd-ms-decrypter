package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import com.opencsv.bean.BeanVerifier;
import com.opencsv.exceptions.CsvConstraintViolationException;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.AdeTransactionsAggregate;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.Set;

/**
 * Implementation of {@link BeanVerifier}, used to verify the validity of the
 * {@link AdeTransactionsAggregate} records extracted from the decrypted file.
 */
public class AdeAggregatesVerifier implements BeanVerifier<AdeTransactionsAggregate> {

  static String malformedFieldMessage = "Malformed field extracted:";
  private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();

  private static final Validator validator = factory.getValidator();

  /**
   * Implementation of {@link BeanVerifier#verifyBean(Object)}, used to verify the validity of the
   * {@link AdeTransactionsAggregate} records extracted from the decrypted file.
   *
   * @param adeTransactionsAggregate The {@link AdeTransactionsAggregate} to be verified
   * @return Boolean representing the validity of the record
   * @throws CsvConstraintViolationException in case of malformed fields
   */
  @Override
  public boolean verifyBean(AdeTransactionsAggregate adeTransactionsAggregate)
      throws CsvConstraintViolationException {
    StringBuilder recordTroubleshootingInfo = new StringBuilder();
    StringBuilder malformedFields = new StringBuilder();

    Set<ConstraintViolation<AdeTransactionsAggregate>> violations = validator.validate(
        adeTransactionsAggregate);

    if (!violations.isEmpty()) {
      Iterator<ConstraintViolation<AdeTransactionsAggregate>> violationIterator = violations.iterator();

      boolean acquirerCodeAppended = false;
      boolean terminalIdAppended = false;
      boolean fiscalCodeAppended = false;

      while (violationIterator.hasNext()) {
        ConstraintViolation<AdeTransactionsAggregate> violation = violationIterator.next();
        String propertyPath = violation.getPropertyPath().toString();

        if (adeTransactionsAggregate.getAcquirerCode() != null
            && !adeTransactionsAggregate.getAcquirerCode().isEmpty() && !acquirerCodeAppended
            && !propertyPath.equals("acquirerCode")) {
          recordTroubleshootingInfo.append(
              String.format("[Acquirer code: %s] ", adeTransactionsAggregate.getAcquirerCode()));
          acquirerCodeAppended = true;
        }
        if (adeTransactionsAggregate.getTerminalId() != null
            && adeTransactionsAggregate.getTerminalId().isEmpty() && !terminalIdAppended
            && !propertyPath.equals("terminalId")) {
          recordTroubleshootingInfo.append(
              String.format("[Terminal id: %s] ", adeTransactionsAggregate.getTerminalId()));
          terminalIdAppended = true;
        }
        if (adeTransactionsAggregate.getFiscalCode() != null
            && adeTransactionsAggregate.getFiscalCode().isEmpty() && !fiscalCodeAppended
            && !propertyPath.equals("fiscalCode")) {
          recordTroubleshootingInfo.append(
              String.format("[Fiscal code: %s] ", adeTransactionsAggregate.getFiscalCode()));
          fiscalCodeAppended = true;
        }

        malformedFields.append(malformedFieldMessage).append(" (")
            .append(violation.getPropertyPath().toString()).append(": ");
        malformedFields.append(violation.getMessage()).append("), ");
      }
    } else {
      recordTroubleshootingInfo.append(
          String.format("[Acquirer code: %s] ", adeTransactionsAggregate.getAcquirerCode()));
      recordTroubleshootingInfo.append(
          String.format("[Terminal id: %s] ", adeTransactionsAggregate.getTerminalId()));
      recordTroubleshootingInfo.append(
          String.format("[Fiscal code: %s] ", adeTransactionsAggregate.getFiscalCode()));
    }

    // Timestamps validity must be verified outside violations iterator

    if (adeTransactionsAggregate.getTransmissionDate() == null || !validateDate(
        adeTransactionsAggregate.getTransmissionDate())) {
      malformedFields.append(malformedFieldMessage).append(" (")
          .append("transmission date").append(": ");
      malformedFields.append("Invalid transmission date ")
          .append(adeTransactionsAggregate.getTransmissionDate())
          .append("), ");
    }
    if (adeTransactionsAggregate.getAccountingDate() == null || !validateDate(
        adeTransactionsAggregate.getAccountingDate())) {
      malformedFields.append(malformedFieldMessage).append(" (")
          .append("accounting date").append(": ");
      malformedFields.append("Invalid accounting date ")
          .append(adeTransactionsAggregate.getAccountingDate())
          .append("), ");
    }

    if (!malformedFields.isEmpty()) {
      throw new CsvConstraintViolationException(
          recordTroubleshootingInfo.append(malformedFields).toString());
    }

    return true;
  }

  private boolean validateDate(String date) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    try {
      LocalDate.parse(date, formatter);
      return true;
    } catch (DateTimeParseException e) {
      return false;
    }
  }
}
