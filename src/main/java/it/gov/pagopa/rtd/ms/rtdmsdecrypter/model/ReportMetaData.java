package it.gov.pagopa.rtd.ms.rtdmsdecrypter.model;

import java.time.LocalDate;
import java.util.HashSet;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportMetaData {
  private HashSet<String> merchantList;
  private long numCanceledTrx;
  private long numPositiveTrx;
  private long totalAmountCanceledTrx;
  private long totalAmountPositiveTrx;
  private LocalDate minAccountingDate;
  private LocalDate maxAccountingDate;
  private String checkSum;

  public ReportMetaData() {
    this.merchantList = new HashSet<>();
    this.numCanceledTrx = 0L;
    this.numPositiveTrx = 0L;
    this.totalAmountCanceledTrx = 0L;
    this.totalAmountPositiveTrx = 0L;
    this.minAccountingDate = LocalDate.MAX;
    this.maxAccountingDate = LocalDate.MIN;
    this.checkSum = "";
  }
}
