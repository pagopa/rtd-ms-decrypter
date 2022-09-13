package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;

/**
 * Interface used to verify the validity of the {@link BlobApplicationAware} records extracted from
 * the input file.
 */
public interface BlobVerifier {

  BlobApplicationAware verify(BlobApplicationAware blob);
}
