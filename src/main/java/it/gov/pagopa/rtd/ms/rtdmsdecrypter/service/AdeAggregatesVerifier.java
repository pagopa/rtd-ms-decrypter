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
import java.util.Set;

/**
 * Implementation of {@link BeanVerifier}, used to verify the validity of the
 * {@link AdeTransactionsAggregate} records extracted from the decrypted file.
 */
public class AdeAggregatesVerifier implements BeanVerifier<AdeTransactionsAggregate> {

  private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();

  private static final Validator validator = factory.getValidator();

  /**
   * Implementation of {@link BeanVerifier#verifyBean(Object)}, used to verify the
   * validity of the
   * {@link AdeTransactionsAggregate} records extracted from the decrypted file.
   *
   * @param adeTransactionsAggregate The {@link AdeTransactionsAggregate} to be
   *                                 verified
   * @return Boolean representing the validity of the record
   * @throws CsvConstraintViolationException in case of malformed fields
   */
  @Override
  public boolean verifyBean(AdeTransactionsAggregate adeTransactionsAggregate)
      throws CsvConstraintViolationException {
    Set<ConstraintViolation<AdeTransactionsAggregate>> violations = validator.validate(
        adeTransactionsAggregate);

    if (!violations.isEmpty()) {
      StringBuilder malformedFields = new StringBuilder();
      for (ConstraintViolation<AdeTransactionsAggregate> violation : violations) {
        if (!violation.getPropertyPath().toString().equals("acquirerCode")) {
          malformedFields.append(String.format("[ Acquirer code: %s ] ",
              adeTransactionsAggregate.getAcquirerCode()));
        }
        if (!violation.getPropertyPath().toString().equals("terminalId")) {
          malformedFields.append(String.format("[ Terminal id: %s ] ",
              adeTransactionsAggregate.getTerminalId()));
        }
        if (!violation.getPropertyPath().toString().equals("fiscalCode")) {
          malformedFields.append(String.format("[ Fiscal code: %s ] ",
              adeTransactionsAggregate.getFiscalCode()));
        }
        malformedFields.append("Malformed fields extracted : (")
            .append(violation.getPropertyPath().toString()).append(": ");
        malformedFields.append(violation.getMessage()).append(") ");
      }

      throw new CsvConstraintViolationException(malformedFields.toString());
    }

    if (!validateDate(adeTransactionsAggregate.getTransmissionDate())) {
      throw new CsvConstraintViolationException(
          "Invalid transmission date " + adeTransactionsAggregate.getTransmissionDate());

    }
    if (!validateDate(adeTransactionsAggregate.getAccountingDate())) {
      throw new CsvConstraintViolationException(
          "Invalid accounting date " + adeTransactionsAggregate.getAccountingDate());
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
