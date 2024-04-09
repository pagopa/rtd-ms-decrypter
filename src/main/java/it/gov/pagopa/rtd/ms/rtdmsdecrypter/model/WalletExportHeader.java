package it.gov.pagopa.rtd.ms.rtdmsdecrypter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletExportHeader {

  @JsonProperty("file_id")
  @NotNull
  private String fileId;

  @JsonProperty("processing_start_time")
  @NotNull
  private String processingStartTime;

  @JsonProperty("processing_end_time")
  @NotNull
  private String processingEndTime;

  @JsonProperty("export_id")
  @NotNull
  private String exportId;

  @JsonProperty("import_file_id")
  @NotNull
  private String importFileId;

  @JsonProperty("extraction_time")
  @NotNull
  private String extractionTime;

  @JsonProperty("contract_quantity")
  @Positive
  private int contractQuantity;

  @JsonProperty("file_sequence_number")
  @NotNull
  private String fileSequenceNumber;

}
