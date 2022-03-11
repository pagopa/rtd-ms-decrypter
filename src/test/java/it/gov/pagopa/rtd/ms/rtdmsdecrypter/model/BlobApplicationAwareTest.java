package it.gov.pagopa.rtd.ms.rtdmsdecrypter.model;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class BlobApplicationAwareTest {

  @Test
  void shouldMatchRegexRTD() {
    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp";
    String blobUri = "/blobServices/default/containers/" + container + "/blobs/" + blob;
    BlobApplicationAware myBlob = new BlobApplicationAware(blobUri);
    assertSame(BlobApplicationAware.Application.RTD, myBlob.getApp());
  }

  @Test
  void shouldMatchRegexADE() {
    String container = "ade-transactions-xxxxxxxxxx8u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "ADE.45678.TRNLOG.20220228.103107.001.csv.pgp";
    String blobUri = "/blobServices/default/containers/" + container + "/blobs/" + blob;
    BlobApplicationAware myBlob = new BlobApplicationAware(blobUri);
    assertSame(BlobApplicationAware.Application.ADE, myBlob.getApp());
  }

  @Test
  void shouldMatchRegexNoApp() {
    String container = "rtd-loremipsum-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp";
    String blobUri = "/blobServices/default/containers/" + container + "/blobs/" + blob;
    BlobApplicationAware myBlob = new BlobApplicationAware(blobUri);
    assertSame(BlobApplicationAware.Application.NOAPP, myBlob.getApp());
  }

  @Test
  void shouldNotMatchRegexNo() {
    String container = "myContainer";
    String blob = "myBlob";
    String blobUri = "/blobServices/default/containers/" + container + "/blobs/" + blob;
    BlobApplicationAware myBlob = new BlobApplicationAware(blobUri);
    assertSame(BlobApplicationAware.Application.NOAPP, myBlob.getApp());
  }

  @Test
  void shouldNotMatchUri() {
    String container = "myContainer";
    String blob = "myBlob";
    String blobUri = "/blobServices/default/directories/" + container + "/blobs/" + blob;
    BlobApplicationAware myBlob = new BlobApplicationAware(blobUri);
    assertSame(BlobApplicationAware.Application.NOAPP, myBlob.getApp());
  }
}
