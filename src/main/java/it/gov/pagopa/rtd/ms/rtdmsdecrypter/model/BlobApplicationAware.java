package it.gov.pagopa.rtd.ms.rtdmsdecrypter.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@NoArgsConstructor
@Slf4j
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

  private Pattern uriPattern = Pattern.compile(
      "^.*containers/((ade|rtd)-transactions-[a-z0-9]{44})/blobs/((ADE|CSTAR)(\\.)(.*))");

  private static final String WRONG_FORMAT_NAME_WARNING_MSG = "Wrong name format:";

  public BlobApplicationAware(String uri) {
    blobUri = uri;
    status = Status.INIT;

    Matcher matcher = uriPattern.matcher(uri);

    if (matcher.matches()) {

      status = Status.RECEIVED;
      container = matcher.group(1);
      blob = matcher.group(3);

      if (checkNameFormat(matcher.group(6).split("\\."))) {

        if (matcher.group(2).equalsIgnoreCase("ADE") && matcher.group(4).equalsIgnoreCase("ADE")) {
          app = Application.ADE;
          targetContainer = targetContainerAde;
        } else if (matcher.group(2).equalsIgnoreCase("RTD") && matcher.group(4)
            .equalsIgnoreCase("CSTAR")) {
          app = Application.RTD;
          targetContainer = targetContainerRtd;
        } else {
          log.warn(WRONG_FORMAT_NAME_WARNING_MSG + blobUri);
          app = Application.NOAPP;
        }
      } else {
        log.warn(WRONG_FORMAT_NAME_WARNING_MSG + blobUri);
        app = Application.NOAPP;
      }
    } else {
      log.warn(WRONG_FORMAT_NAME_WARNING_MSG + blobUri);
      app = Application.NOAPP;
    }
  }

  /**
   * This method matches PagoPA file name's standard Specifics can be found at:
   * https://docs.pagopa.it/digital-transaction-register/v/digital-transaction-filter/acquirer-integration-with-pagopa-centrostella/integration/standard-pagopa-file-transactions
   *
   * @param uriTokens values obtained from the name of the blob (separated by dots)
   * @return true if the name matches the format, false otherwise
   */
  private boolean checkNameFormat(String[] uriTokens) {
    // Check for sender ABI code
    if (uriTokens[0] == null || !uriTokens[0].matches("[a-zA-Z0-9]{5}")) {
      return false;
    }

    // Check for filetype (fixed "TRNLOG" value)
    // Should ignore case?
    if (uriTokens[1] == null || !uriTokens[1].equalsIgnoreCase("TRNLOG")) {
      return false;
    }

    // Check for creation timestamp correctness
    if (uriTokens[2] == null || uriTokens[3] == null) {
      return false;
    }

    SimpleDateFormat daysFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    // Make the format refuse wrong date and time (default behavior is to overflow values in
    // following date)
    daysFormat.setLenient(false);

    try {
      daysFormat.parse(uriTokens[2] + uriTokens[3]);
    } catch (ParseException e) {
      return false;
    }

    // Check for progressive value
    return (uriTokens[4] != null) && uriTokens[4].matches("[0-9]{3}");
  }

}
  

