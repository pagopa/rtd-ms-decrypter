package it.gov.pagopa.rtd.ms.rtdmsdecrypter.telemetry;

import com.azure.monitor.opentelemetry.exporter.AzureMonitorExporter;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty(value = "applicationinsights.enabled", havingValue = "true", matchIfMissing = false)
@Import(SpringCloudKafkaBinderInstrumentation.class)
public class AppInsightConfig {

  @Bean
  public AutoConfigurationCustomizerProvider otelCustomizer(
          @Value("${applicationinsights.connection-string}") String connectionString
  ) {
    return p -> {
      if (p instanceof AutoConfiguredOpenTelemetrySdkBuilder builder) {
        AzureMonitorExporter.customize(
                builder,
                connectionString
        );
      }
    };
  }

}
