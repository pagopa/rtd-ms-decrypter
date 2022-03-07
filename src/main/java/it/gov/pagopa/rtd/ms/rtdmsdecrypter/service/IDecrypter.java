package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;

public interface IDecrypter {
  BlobApplicationAware decrypt(BlobApplicationAware blob);

}
