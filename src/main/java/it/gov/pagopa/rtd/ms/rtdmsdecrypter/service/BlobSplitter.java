package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import java.io.IOException;
import java.util.List;

/**
 * Interface that splits the content of a blob storage into arbitrary smaller chunks (blobs).
 */
public interface BlobSplitter {

  List<BlobApplicationAware> split(BlobApplicationAware blob, int n);
}
