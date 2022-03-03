package it.gov.pagopa.rtd.ms.rtdmsdecrypter.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;

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

  @Value("${decrypt.blobclient.targetContainer.ade}")
  private String targetContainerAde;

  @Value("${decrypt.blobclient.targetContainer.rtd}")
  private String targetContainerRtd;

  private Pattern uriPattern = Pattern.compile("^.*containers/((ade|rtd)-transactions-[a-z0-9]{44})/blobs/((ADE|CSTAR).*)");


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
  

