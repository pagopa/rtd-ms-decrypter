package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import com.opencsv.bean.BeanVerifier;
import com.opencsv.exceptions.CsvConstraintViolationException;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.AdeTransactionsAggregate;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

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
        malformedFields.append("(").append(String.format(" Terminal ID: %s ", adeTransactionsAggregate.getTerminalId()))
            .append(violation.getPropertyPath().toString()).append(": ");
        malformedFields.append(violation.getMessage()).append(") ");
      }

      throw new CsvConstraintViolationException("Malformed fields extracted: "
          + malformedFields);
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
