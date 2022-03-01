package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BlobRestConnector implements IBlobRestConnector {
  public String get(String blobUri) {
    return blobUri;
  }

  public String put(String blobUri) {
    return blobUri;
  }

}
