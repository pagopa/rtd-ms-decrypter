package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;

/**
 * Interface offering PGP decryption methods.
 */
public interface Decrypter {

  BlobApplicationAware decrypt(BlobApplicationAware blob);

}
