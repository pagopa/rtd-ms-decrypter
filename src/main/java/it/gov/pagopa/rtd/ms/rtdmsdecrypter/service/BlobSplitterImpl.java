package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import static it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Status.SPLIT;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Application;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Concrete implementation of a BlobSplitter interface.
 */
@Service
@Setter
@Slf4j
public class BlobSplitterImpl implements BlobSplitter {

  // Max number of lines allowed in one blob chunk.
  @Value("${decrypt.splitter.threshold}")
  private int lineThreshold;

  private String decryptedSuffix = ".decrypted";

  @Value("${decrypt.skipChecksum}")
  private boolean checksumSkipped;

  /**
   * Method that split the content of a blob in chunks of n lines.
   *
   * @param blob to be split.
   * @return a list of blobs that represent the split blob.
   */
  public Stream<BlobApplicationAware> split(BlobApplicationAware blob) {
    log.info("Start splitting blob {} from {}", blob.getBlob(), blob.getContainer());

    String blobPath = Path.of(blob.getTargetDir(), blob.getBlob() + decryptedSuffix).toString();

    ArrayList<BlobApplicationAware> blobSplit = new ArrayList<>();

    // Flag for fail split
    boolean failSplit = false;

    // Flag for skipping first checksum line
    checksumSkipped = false;

    // Incremental integer for chunk numbering
    int chunkNum = 0;

    String chunkName;

    try (
        LineIterator it = FileUtils.lineIterator(
            Path.of(blobPath).toFile(), "UTF-8")) {
      if (it.hasNext() && !checksumSkipped) {
        String line = it.nextLine();
        log.info("Checksum: {} {}", blob.getBlob(), line);
        blob.getReportMetaData().setCheckSum(line);
        checksumSkipped = true;
      }
      while (it.hasNext()) {
        if (blob.getApp() == Application.ADE) {
          // Left pad with 0s the chunk number to 3 char
          chunkName = adeNamingConvention(blob) + String.format("%03d", chunkNum);
        } else {
          chunkName = blob.getBlob() + "." + chunkNum + decryptedSuffix;
        }
        try (Writer writer = Channels.newWriter(new FileOutputStream(
            Path.of(blob.getTargetDir(), chunkName).toString(),
            true).getChannel(),
            StandardCharsets.UTF_8)) {
          writeChunks(it, writer, blob);
          BlobApplicationAware tmpBlob = new BlobApplicationAware(
              blob.getBlobUri());
          tmpBlob.setOriginalBlob(blob);
          tmpBlob.setOriginalBlobName(blob.getBlob());
          tmpBlob.setStatus(SPLIT);
          tmpBlob.setApp(blob.getApp());
          tmpBlob.setBlob(chunkName);
          tmpBlob.setBlobUri(
              blob.getBlobUri().substring(0, blob.getBlobUri().lastIndexOf("/")) + "/" + chunkName);
          tmpBlob.setNumChunk(chunkNum);
          blobSplit.add(tmpBlob);
        }

        chunkNum++;
      }
      for (BlobApplicationAware blobApplicationAware : blobSplit) {
        blobApplicationAware.setTotChunk(chunkNum);
      }

    } catch (IOException e) {
      log.error("Missing blob file:{}", blobPath);
      failSplit = true;
    }

    return finalizeSplit(blob, failSplit, chunkNum, blobSplit);
  }

  private String adeNamingConvention(BlobApplicationAware blob) {
    // Note that no chunk number is added to the blob name
    return "AGGADE." + blob.getSenderCode() + "." + blob.getFileCreationDate() + "."
        + blob.getFileCreationTime() + "." + blob.getFlowNumber() + "."
        + blob.getBatchServiceChunkNumber();
  }

  private void writeChunks(LineIterator it, Writer writer,
      BlobApplicationAware blob)
      throws IOException {
    // Counter for current line number (from 0 to lineThreshold)
    int i = 0;
    while (it.hasNext() && i < this.lineThreshold) {
      writer.append(it.nextLine()).append("\n");
      i++;
    }
  }

  private Stream<BlobApplicationAware> finalizeSplit(BlobApplicationAware blob, boolean failSplit,
      int chunkNum, ArrayList<BlobApplicationAware> blobSplit) {

    if (!failSplit) {
      log.info("Obtained {} chunk/s from blob:{}", chunkNum, blob.getBlob());
      for (BlobApplicationAware b : blobSplit) {
        b.setOriginalFileChunksNumber(chunkNum);
      }
      return blobSplit.stream();
    } else {
      // If split fails, return the original blob (without the SPLIT status)
      log.info("Failed splitting blob:{}", blob.getBlob());
      blob.localCleanup();
      return Stream.of(blob);
    }
  }
}
