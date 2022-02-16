package it.gov.pagopa.rtd.ms.rtdmsdecrypter.handlers;

import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public
class BlobStorage {
  
  @Bean
  public Consumer<String> decrypt() {
    return message -> System.out.println("Received message " + message);
  }
}