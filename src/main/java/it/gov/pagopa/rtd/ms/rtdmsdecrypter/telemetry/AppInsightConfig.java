package it.gov.pagopa.rtd.ms.rtdmsdecrypter.telemetry;

import com.azure.monitor.opentelemetry.exporter.AzureMonitorExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty(value = "applicationinsights.enabled", havingValue = "true", matchIfMissing = false)
@Import(SpringCloudKafkaBinderInstrumentation.class)
public class AppInsightConfig implements BeanPostProcessor {

  private final AzureMonitorExporterBuilder azureMonitorExporterBuilder;

  public AppInsightConfig(
      @Value("${applicationinsights.connection-string}") String applicationInsights) {
    this.azureMonitorExporterBuilder = new AzureMonitorExporterBuilder().connectionString(
        applicationInsights);
  }

  @Bean
  public AutoConfigurationCustomizerProvider otelCustomizer() {
    return p -> {
      if (p instanceof AutoConfiguredOpenTelemetrySdkBuilder) {
        this.azureMonitorExporterBuilder.install((AutoConfiguredOpenTelemetrySdkBuilder) p);
      }
    };
  }
}
