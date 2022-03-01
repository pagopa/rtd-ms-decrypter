package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

public interface IBlobRestConnector {
  // Downloads a blob and returns the name of the downloaded file
  public String get(String blobUri);
  // Uploads a blob and returns the name of the uploaded file
  public String put(String blobName);
}