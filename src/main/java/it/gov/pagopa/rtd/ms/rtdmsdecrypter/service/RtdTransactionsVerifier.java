package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import com.opencsv.bean.BeanVerifier;
import com.opencsv.exceptions.CsvConstraintViolationException;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.RtdTransaction;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;

/**
 * Implementation of {@link BeanVerifier}, used to verify the validity of the
 * {@link RtdTransaction}
 * records extracted from the decrypted file.
 */
public class RtdTransactionsVerifier implements BeanVerifier<RtdTransaction> {

  private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();

  private static final Validator validator = factory.getValidator();

  @Override
  public boolean verifyBean(RtdTransaction rtdTransactions) throws CsvConstraintViolationException {
    Set<ConstraintViolation<RtdTransaction>> violations = validator.validate(
        rtdTransactions);

    if (!violations.isEmpty()) {
      StringBuilder malformedFields = new StringBuilder();
      for (ConstraintViolation<RtdTransaction> violation : violations) {
        if (!violation.getPropertyPath().toString().equals("acquirerCode")) {
          malformedFields.append(String.format("[ Acquirer code: %s ] ",
              rtdTransactions.getAcquirerCode()));
        }
        if (!violation.getPropertyPath().toString().equals("terminalId")) {
          malformedFields.append(String.format("[ Terminal id: %s ] ",
              rtdTransactions.getTerminalId()));
        }
        if (!violation.getPropertyPath().toString().equals("fiscalCode")) {
          malformedFields.append(String.format("[ Fiscal code: %s ] ",
              rtdTransactions.getFiscalCode()));
        }
        malformedFields.append("Malformed fields extracted : (")
            .append(violation.getPropertyPath().toString()).append(": ");
        malformedFields.append(violation.getMessage()).append(") ");
      }

      throw new CsvConstraintViolationException(malformedFields.toString());
    }

    return true;
  }
}
