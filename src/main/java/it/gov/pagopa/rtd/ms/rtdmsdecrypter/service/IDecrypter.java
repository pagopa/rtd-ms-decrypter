package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import java.io.InputStream;
import java.io.OutputStream;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;

public interface IDecrypter {
  BlobApplicationAware decrypt(BlobApplicationAware fileName);

  void decryptFile(InputStream input, OutputStream output);
}
