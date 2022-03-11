package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;

public interface BlobRestConnector {

  // Downloads a blob and returns the name of the downloaded file
  BlobApplicationAware get(BlobApplicationAware blobUri);

  // Uploads a blob and returns the name of the uploaded file
  BlobApplicationAware put(BlobApplicationAware fileName);

}