package it.gov.pagopa.rtd.ms.rtdmsdecrypter.model;

import java.time.LocalDate;
import java.util.HashSet;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportMetaData {
  private HashSet<String> numMerchant;
  private int numCancelledTrx;
  private int numPositiveTrx;
  private long totalAmountCancelledTrx;
  private long totalAmountPositiveTrx;
  private LocalDate minAccountingDate;
  private LocalDate maxAccountingDate;

  public ReportMetaData() {
    this.numMerchant = new HashSet<String>();
    this.numCancelledTrx = 0;
    this.numPositiveTrx = 0;
    this.totalAmountCancelledTrx = 0;
    this.totalAmountPositiveTrx = 0;
    this.minAccountingDate = LocalDate.MAX;
    this.maxAccountingDate = LocalDate.MIN;
  }
}
