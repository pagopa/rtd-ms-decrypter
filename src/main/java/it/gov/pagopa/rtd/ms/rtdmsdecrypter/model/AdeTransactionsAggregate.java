package it.gov.pagopa.rtd.ms.rtdmsdecrypter.model;

import com.opencsv.bean.CsvBindByPosition;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model for transactions' POS-based aggregates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdeTransactionsAggregate {

  @NotNull
  @NotBlank
  @Size(max = 20)
  @CsvBindByPosition(position = 0)
  String acquirerCode;

  @NotNull
  @NotBlank
  @Size(min = 2, max = 2)
  @Pattern(regexp = "\\d{2}")
  @CsvBindByPosition(position = 1)
  String operationType;

  @NotNull
  @NotBlank
  @CsvBindByPosition(position = 2)
  String transmissionDate;

  @NotNull
  @NotBlank
  @CsvBindByPosition(position = 3)
  String accountingDate;

  @NotNull
  @CsvBindByPosition(position = 4)
  @Min(value = 1, message = "The number of transactions must be positive")
  int numTrx;

  @NotNull
  @CsvBindByPosition(position = 5)
  @Min(value = 1L, message = "The total amount must be positive")
  Long totalAmount;

  @NotNull
  @NotBlank
  @Size(max = 3)
  @Pattern(regexp = "978")
  @CsvBindByPosition(position = 6)
  String currency;

  @NotNull
  @NotBlank
  @Size(max = 255)
  @CsvBindByPosition(position = 7)
  String acquirerId;

  @NotNull
  @NotBlank
  @Size(max = 255)
  @CsvBindByPosition(position = 8)
  String merchantId;

  @NotNull
  @NotBlank
  @Size(max = 255)
  @CsvBindByPosition(position = 9)
  String terminalId;

  @NotNull
  @NotBlank
  @Size(max = 50)
  @CsvBindByPosition(position = 10)
  String fiscalCode;

  @Size(max = 50)
  @CsvBindByPosition(position = 11)
  String vat;

  @NotNull
  @NotBlank
  @Size(min = 2, max = 2)
  @Pattern(regexp = "00|01|99")
  @CsvBindByPosition(position = 12)
  String posType;

}
