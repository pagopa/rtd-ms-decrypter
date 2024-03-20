package it.gov.pagopa.rtd.ms.rtdmsdecrypter.model;

import java.time.LocalDate;
import java.util.HashSet;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportMetaData {
  private HashSet<String> merchantList;
  private long numCancelledTrx;
  private long numPositiveTrx;
  private long totalAmountCancelledTrx;
  private long totalAmountPositiveTrx;
  private LocalDate minAccountingDate;
  private LocalDate maxAccountingDate;
  private String checkSum;

  public ReportMetaData() {
    this.merchantList = new HashSet<>();
    this.numCancelledTrx = 0L;
    this.numPositiveTrx = 0L;
    this.totalAmountCancelledTrx = 0L;
    this.totalAmountPositiveTrx = 0L;
    this.minAccountingDate = LocalDate.MAX;
    this.maxAccountingDate = LocalDate.MIN;
    this.checkSum = "";
  }
}
