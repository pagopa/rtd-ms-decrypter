package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import java.io.InputStream;
import java.io.OutputStream;

public interface IDecrypter {
  String decrypt(String fileName);

  void decryptFile(InputStream input, OutputStream output);
}
