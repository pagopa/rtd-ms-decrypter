package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import com.opencsv.bean.BeanVerifier;
import com.opencsv.exceptions.CsvConstraintViolationException;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.RtdTransaction;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

/**
 * Implementation of {@link BeanVerifier}, used to verify the validity of the {@link RtdTransaction}
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
        malformedFields.append("(").append(violation.getPropertyPath().toString()).append(": ");
        malformedFields.append(violation.getMessage()).append(") ");
      }

      throw new CsvConstraintViolationException("Malformed fields extracted: "
          + malformedFields);
    }

    return true;
  }
}
