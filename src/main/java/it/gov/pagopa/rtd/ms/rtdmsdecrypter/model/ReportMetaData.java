package it.gov.pagopa.rtd.ms.rtdmsdecrypter.model;

import java.time.LocalDateTime;
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
  private LocalDateTime minAccountingDate;
  private LocalDateTime maxAccountingDate;
  private String checkSum;

  public ReportMetaData() {
    this.merchantList = new HashSet<>();
    this.numCanceledTrx = 0L;
    this.numPositiveTrx = 0L;
    this.totalAmountCanceledTrx = 0L;
    this.totalAmountPositiveTrx = 0L;
    this.minAccountingDate = LocalDateTime.MAX;
    this.maxAccountingDate = LocalDateTime.MIN;
    this.checkSum = "";
  }

  public void updateAccountingDate(LocalDateTime accountingDate) {
    if (this.minAccountingDate.isAfter(accountingDate)) {
      this.setMinAccountingDate(accountingDate);
    }
    if (this.maxAccountingDate.isBefore(accountingDate)) {
      this.setMaxAccountingDate(accountingDate);
    }
  }

  public void increaseTrx(String operationType, int numTrx) {
    if (operationType.equals("00")) {
      setNumPositiveTrx(this.numPositiveTrx + numTrx);
    } else {
      setNumCanceledTrx(this.numCanceledTrx + numTrx);
    }
  }

  public void increaseTotalAmountTrx(String operationType, long totalAmount) {
    if (operationType.equals("00")) {
      setTotalAmountPositiveTrx(this.totalAmountPositiveTrx + totalAmount);
    } else {
      setTotalAmountCanceledTrx(this.totalAmountCanceledTrx + totalAmount);
    }
  }

}
