package it.gov.pagopa.rtd.ms.rtdmsdecrypter.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BlobApplicationAware {

  public enum Application {
    RTD,
    ADE,
    NOAPP
  }

  public enum Status {
    INIT,
    RECEIVED,
    DOWNLOADED,
    DECRYPTED,
    UPLOADED
  }
  
  private String blobUri;
  private String container;
  private String blob;
  private Application app;
  private Status status;
  private String targetContainer;


  private String targetContainerAde = "ade-transactions-decrypted";
  private String targetContainerRtd = "rtd-transactions-decrypted";

  private String targetDir = "/tmp";

  //Note: This pattern does not support leap years
  //TODO Maybe a check for the presence of a real ABI code should be performed
  //The following pattern matches PagoPA file name's standard
  //  Specifics can be found at: https://docs.pagopa.it/digital-transaction-register/v/digital-transaction-filter/acquirer-integration-with-pagopa-centrostella/integration/standard-pagopa-file-transactions
  // It is separated in lines to be easily read and maintained:
  //  URI (of the blob)
  //  APP. (ADE or CSTAR)
  //  ABI.
  //  TRNLOG. (fixed)
  //  YYYYMMDD.
  //  HHMISS.
  //  nnn.  (max 999 transaction with the same previous fields)
  //  .*  (matches everything e.g. file extension)
  private Pattern uriPattern = Pattern.compile("^.*containers/((ade|rtd)-transactions-[a-z0-9]{44})/blobs/" +
          "((ADE|CSTAR)\\." +
          "([0-9]{5})\\." +
          "(TRNLOG)\\." +
          "((?:(?:(?:(?:(?:[13579][26]|[2468][048])00)|(?:[0-9]{2}(?:(?:[13579][26])|(?:[2468][048]|0[48]))))(?:(?:(?:09|04|06|11)(?:0[1-9]|1[0-9]|2[0-9]|30))|(?:(?:01|03|05|07|08|10|12)(?:0[1-9]|1[0-9]|2[0-9]|3[01]))|(?:02(?:0[1-9]|1[0-9]|2[0-9]))))|(?:[0-9]{4}(?:(?:(?:09|04|06|11)(?:0[1-9]|1[0-9]|2[0-9]|30))|(?:(?:01|03|05|07|08|10|12)(?:0[1-9]|1[0-9]|2[0-9]|3[01]))|(?:02(?:[01][0-9]|2[0-8]))))))\\." +
          "((?:0[0-9]|1[0-9]|2[0-3])(?:[0-5][0-9]){2})\\." +
          "([0-9]{3})\\." +
          ".*)");


  public BlobApplicationAware(String uri) {
    blobUri = uri;
    status = Status.INIT;

    Matcher matcher = uriPattern.matcher(uri);
    if (matcher.matches()) {

      status = Status.RECEIVED;
      container = matcher.group(1);
      blob = matcher.group(3);

      if (matcher.group(2).equalsIgnoreCase("ADE") && matcher.group(4).equalsIgnoreCase("ADE")) {
        app = Application.ADE;
        targetContainer = targetContainerAde;
      } else if (matcher.group(2).equalsIgnoreCase("RTD") && matcher.group(4).equalsIgnoreCase("CSTAR")) {
        app = Application.RTD;
        targetContainer = targetContainerRtd;
      } else {
        app = Application.NOAPP;
      }
    }
    else {
      app = Application.NOAPP;
    }
  }

}
  

