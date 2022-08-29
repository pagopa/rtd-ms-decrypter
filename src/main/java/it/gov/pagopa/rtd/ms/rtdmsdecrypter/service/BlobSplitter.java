package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import java.util.stream.Stream;

/**
 * Interface that splits the content of a blob storage into arbitrary smaller chunks (blobs).
 */
public interface BlobSplitter {

  Stream<BlobApplicationAware> split(BlobApplicationAware blob);
}
