package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class Decrypter {
  public void decrypt(String blob) {
    log.info("Decrypted Blob {}.", blob);
  }
}
