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
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
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

  //Max number of lines allowed in one blob chunk.
  @Value("${decrypt.splitter.threshold}")
  private int lineThreshold;

  private String decryptedSuffix = ".decrypted";

  private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();

  private static final Validator validator = factory.getValidator();

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

    //Flag for fail split
    boolean failSplit = false;

    //Flag for skipping first checksum line
    boolean checksumSkipped = false;

    //Incremental integer for chunk numbering
    int chunkNum = 0;

    // Counter for current line number (from 0 to n)
    int i;

    String chunkName;

    try (
        LineIterator it = FileUtils.lineIterator(
            Path.of(blobPath).toFile(), "UTF-8")
    ) {
      while (it.hasNext()) {
        if (blob.getApp() == Application.ADE) {
          chunkName = adeNamingConvention(blob) + "." + chunkNum;
        } else {
          chunkName = blob.getBlob() + "." + chunkNum + decryptedSuffix;
        }
        try (Writer writer = Channels.newWriter(new FileOutputStream(
                Path.of(blob.getTargetDir(), chunkName).toString(),
                true).getChannel(),
            StandardCharsets.UTF_8)) {
          i = 0;
          while (i < lineThreshold) {
            if (it.hasNext()) {
              String line = it.nextLine();
              //Skip the checksum line (the first one)
              if (!checksumSkipped) {
                log.info("Checksum: {} {}", blob.getBlob(), line);
                checksumSkipped = true;
                i--;
              } else {
                writer.append(line).append("\n");
              }
            } else {
              break;
            }
            i++;
          }
          BlobApplicationAware tmpBlob = new BlobApplicationAware(
              blob.getBlobUri());
          tmpBlob.setOriginalBlobName(blob.getBlob());
          tmpBlob.setStatus(SPLIT);
          tmpBlob.setApp(blob.getApp());
          tmpBlob.setBlob(chunkName);
          tmpBlob.setBlobUri(
              blob.getBlobUri().substring(0, blob.getBlobUri().lastIndexOf("/")) + "/" + chunkName);
          blobSplit.add(tmpBlob);
        }
        chunkNum++;
      }
    } catch (IOException e) {
      log.error("Missing blob file:{}", blobPath);
      failSplit = true;
    }

    if (!failSplit) {
      log.info("Obtained {} chunk/s from blob:{}", chunkNum, blob.getBlob());
      for (BlobApplicationAware b : blobSplit) {
        b.setOrigianalFileChunksNumber(chunkNum);
      }
      return blobSplit.stream();
    } else {
      // If split fails, return the original blob (without the SPLIT status)
      log.info("Failed splitting blob:{}", blob.getBlob());
      return Stream.of(blob);
    }
  }

  private String adeNamingConvention(BlobApplicationAware blob) {
    // Note that no chunk number is added to the blob name
    return "AGGADE." + blob.getSenderCode() + "." + blob.getFileCreationDate() + "."
        + blob.getFileCreationTime() + "." + blob.getFlowNumber();
  }
}
