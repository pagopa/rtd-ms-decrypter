package it.gov.pagopa.rtd.ms.rtdmsdecrypter.config;

import com.opencsv.bean.BeanVerifier;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.AdeTransactionsAggregate;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Application;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.DecryptedRecord;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.RtdTransaction;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.AdeAggregatesVerifier;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.RtdTransactionsVerifier;
import org.springframework.context.annotation.Configuration;

/**
 * Factory class for blob validator a blob.
 */
@Configuration
public class VerifierFactory {

  /**
   * Returns a {@link BeanVerifier} instance, based on the {@link Application} of the passed as
   * parameter.
   *
   * @param app {@link Application} of the blob to be verified.
   * @return BeanVerifier
   */
  public BeanVerifier<? extends DecryptedRecord> getVerifier(Application app) {
    if (app == Application.ADE) {
      return new AdeAggregatesVerifier();
    }
    return new RtdTransactionsVerifier();
  }

  /**
   * Returns a {@link Class} based on the {@link Application} of the passed as parameter.
   *
   * @param app {@link Application} of the blob to be verified.
   * @return BeanVerifier
   */
  public Class<? extends DecryptedRecord> getBeanClass(Application app) {
    if (app == Application.ADE) {
      return AdeTransactionsAggregate.class;
    }
    return RtdTransaction.class;
  }
}
