package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;

public interface IBlobRestConnector {
  // Downloads a blob and returns the name of the downloaded file
  public BlobApplicationAware get(BlobApplicationAware blobUri);
  // Uploads a blob and returns the name of the uploaded file
  public BlobApplicationAware put(BlobApplicationAware fileName);

}