package it.gov.pagopa.rtd.ms.rtdmsdecrypter.model;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Class representing a blob-stored transactions file, and it's processing state.
 */
@Getter
@Setter
@Slf4j
public class BlobApplicationAware {

  /**
   * Enumeration of managed applications (i.e. 'verticals')
   */
  public enum Application {
    RTD,
    ADE,
    WALLET,
    NOAPP
  }

  /**
   * File lifecycle statuses.
   */
  public enum Status {
    INIT,
    RECEIVED,
    DOWNLOADED,
    DECRYPTED,
    VERIFIED,
    SPLIT,
    UPLOADED,
    ENRICHED,
    DELETED
  }

  private static final int RTD_NAME_CHUNK_NUM = 7;
  private static final int WALLET_NAME_CHUNK_NUM = 6;

  private String blobUri;
  private String container;
  private String blob;
  private Application app;
  private Status status;
  private String targetContainer;
  private String originalBlobName;
  private BlobApplicationAware originalBlob;

  private String senderCode;
  private String fileCreationDate;
  private String fileCreationTime;

  private String flowNumber;

  private String batchServiceChunkNumber;

  private Integer originalFileChunksNumber;

  private String inputContainerWallet = "nexi";

  private String inputDirectoryWallet = "in";

  private String targetContainerAde = "ade-transactions-decrypted";

  private String targetContainerRtd = "rtd-transactions-decrypted";

  private String targetContainerWallet = "wallet-contracts-decrypted";

  private String targetDir = "/tmp";

  private String uploadPrefix = "upload";

  private String walletExportFileSuffix = "OUT";

  private Pattern uriRtdPattern = Pattern.compile(
      "^.*containers/((ade|rtd)(-transactions-[a-z0-9]{44}))/blobs/(.*)");

  private Pattern uriWalletPattern = Pattern.compile(
      "^.*containers/(" + inputContainerWallet + ")/blobs/" + inputDirectoryWallet + "/(.*)");

  private static final String WRONG_FORMAT_NAME_WARNING_MSG = "Wrong name format:";
  private static final String CONFLICTING_SERVICE_WARNING_MSG = "Conflicting service in URI:";
  private static final String EVENT_NOT_OF_INTEREST_WARNING_MSG = "Event not of interest:";
  private static final String FAIL_FILE_DELETE_WARNING_MSG = "Failed to delete local blob file:";

  // Enrich metadata fields
  private ReportMetaData reportMetaData;

  private int numChunk;
  private int totChunk;

  /**
   * Constructor.
   *
   * @param uri the blob URI
   */
  public BlobApplicationAware(String uri) {

    reportMetaData = new ReportMetaData();
    originalBlob = null;
    numChunk = 0;
    totChunk = 0;

    blobUri = uri;
    status = Status.INIT;

    Matcher matcherRtd = uriRtdPattern.matcher(uri);
    Matcher matcherWallet = uriWalletPattern.matcher(uri);

    if (matcherRtd.matches()) {
      container = matcherRtd.group(1);
      blob = matcherRtd.group(4);
      originalBlobName = blob;

      // Tokenized blob name for checking compliance
      String[] blobNameTokenized = blob.split("\\.");

      // Set status, regardless of name correctness
      status = Status.RECEIVED;

      if (checkRtdNameFormat(blobNameTokenized)) {
        // Check whether the blob's service matches in path and name, then assign
        // Application
        if (matcherRtd.group(2).equalsIgnoreCase("ADE") && blobNameTokenized[0]
            .equalsIgnoreCase("ADE")) {
          app = Application.ADE;
          targetContainer = targetContainerAde;
        } else if (matcherRtd.group(2).equalsIgnoreCase("RTD")
            && blobNameTokenized[0].equalsIgnoreCase("CSTAR")) {
          app = Application.RTD;
          targetContainer = targetContainerRtd;
        } else {
          log.warn(CONFLICTING_SERVICE_WARNING_MSG + blobUri);
          app = Application.NOAPP;
        }
      } else {
        log.warn(WRONG_FORMAT_NAME_WARNING_MSG + blobUri);
        app = Application.NOAPP;
      }
      return;
    }

    if (matcherWallet.matches()) {
      container = matcherWallet.group(1);
      blob = matcherWallet.group(2);
      originalBlobName = blob;

      String[] blobNameTokenized = blob.split("_");

      if (filterTemporaryBlobs(blobNameTokenized)) {
        status = Status.RECEIVED;

        if (checkWalletNameFormat(blobNameTokenized)) {
          app = Application.WALLET;
          targetContainer = targetContainerWallet;
        } else {
          log.warn(WRONG_FORMAT_NAME_WARNING_MSG + blobUri);
          app = Application.NOAPP;
        }

        return;
      }
    }

    log.info(EVENT_NOT_OF_INTEREST_WARNING_MSG + blobUri);
    app = Application.NOAPP;
  }

  /**
   * This method matches PagoPA file name's standard Specifics can be found at:
   * https://docs.pagopa.it/digital-transaction-register/v/digital-transaction-filter/acquirer-integration-with-pagopa-centrostella/integration/standard-pagopa-file-transactions
   *
   * @param blobNameTokens values obtained from the name of the blob (separated by dots)
   * @return true if the name matches the format, false otherwise
   */
  private boolean checkRtdNameFormat(String[] blobNameTokens) {

    // blobNameTokens[1] check for sender ABI code
    if (blobNameTokens.length < RTD_NAME_CHUNK_NUM
        || !blobNameTokens[0].matches("(ADE|CSTAR)")
        || !blobNameTokens[1].matches("[a-zA-Z0-9]{5}")) {
      return false;
    }

    senderCode = blobNameTokens[1];

    // Check for filetype (fixed "TRNLOG" value)
    if (!blobNameTokens[2].equalsIgnoreCase("TRNLOG")) {
      return false;
    }

    extractBatchServiceChunkNumber(blobNameTokens[6]);
    return checkDateTimeFormat(blobNameTokens[3], blobNameTokens[4]) && extractFlowNumber(
        blobNameTokens[5]);
  }

  private boolean checkWalletNameFormat(String[] blobNameTokens) {

    if (blobNameTokens.length != WALLET_NAME_CHUNK_NUM
        || !blobNameTokens[0].equals("PAGOPAPM")
        || !blobNameTokens[1].equals("NPG")
        || !blobNameTokens[2].equals("CONTRACTS")) {
      return false;
    }

    return checkDateTimeFormat(blobNameTokens[3].substring(0, 8), blobNameTokens[3].substring(8))
        && extractFlowNumber(blobNameTokens[4]);
  }

  private boolean checkDateTimeFormat(String date, String time) {
    SimpleDateFormat daysFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    // Make the format refuse wrong date and time (default behavior is to overflow
    // values in
    // following date)
    daysFormat.setLenient(false);

    try {
      daysFormat.parse(date + time);
    } catch (ParseException e) {
      log.error("Error parsing date and time: {}", e.getMessage());
      return false;
    }

    fileCreationDate = date;
    fileCreationTime = time;

    return true;
  }

  /**
   * This method deletes the local files left by the blob handling (get, decrypt, split, put).
   *
   * @return the blob with its status set to deleted.
   */
  public BlobApplicationAware localCleanup() {
    log.info("Start deleting locally blob {}", blob);

    File tmpFile = Path.of(targetDir, blob).toFile();
    try {
      // Delete the chunk
      if (tmpFile.exists()) {
        Files.delete(tmpFile.toPath());
      }

      // Delete the original encrypted file (if present)
      tmpFile = Path.of(this.targetDir, originalBlobName).toFile();
      if (tmpFile.exists()) {
        Files.delete(tmpFile.toPath());
      }

      // Delete the original decrypted file (if present)
      tmpFile = Path.of(this.targetDir, originalBlobName + ".decrypted").toFile();
      if (tmpFile.exists()) {
        Files.delete(tmpFile.toPath());
      }

    } catch (Exception e) {
      log.warn(FAIL_FILE_DELETE_WARNING_MSG + tmpFile.getName() + " (" + e + ")");
    }

    status = Status.DELETED;

    log.info("Deleted locally blob {}", blob);
    return this;

  }

  boolean extractFlowNumber(String token) {
    // Check for progressive value
    if (token.matches("\\d{3}")) {
      flowNumber = token;
      return true;
    } else {
      return false;
    }
  }

  void extractBatchServiceChunkNumber(String token) {
    if (token.matches("(\\d{2})")) {
      batchServiceChunkNumber = token;
    } else {
      batchServiceChunkNumber = "00";
    }
  }

  boolean filterTemporaryBlobs(String[] blobNameTokens) {
    if (blobNameTokens.length < WALLET_NAME_CHUNK_NUM) {
      return false;
    }
    // Check if last token contains "upload"
    if (blobNameTokens[0].equals(uploadPrefix)) {
      return false;
    }
    // Check if last token contains ".check"
    if (!blobNameTokens[5].equals(walletExportFileSuffix)) {
      return false;
    }
    log.info(blobNameTokens[5]);
    return true;
  }
}
