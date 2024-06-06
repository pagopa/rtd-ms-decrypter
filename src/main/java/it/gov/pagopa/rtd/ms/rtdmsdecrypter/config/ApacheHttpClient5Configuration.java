package it.gov.pagopa.rtd.ms.rtdmsdecrypter.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.apachehttpclient.v5_2.ApacheHttpClient5Telemetry;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ApacheHttpClient5Configuration {

  private final OpenTelemetry openTelemetry;

  // creates a new http client builder for constructing http clients with open telemetry instrumentation
  @Bean
  public HttpClientBuilder createHttpClientBuilder() {
    return ApacheHttpClient5Telemetry.builder(openTelemetry).build().newHttpClientBuilder();
  }
}
