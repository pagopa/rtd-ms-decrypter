package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
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

  //Max number of lines allowed in one blob chunk.
  @Value("${decrypt.splitter.threshold}")
  private int lineThreshold;

  /**
   * Method that spli the content of a blob in chunks of n lines.
   *
   * @param blob to be split.
   * @return a list of blobs that represent the split blob.
   */
  public Stream<BlobApplicationAware> split(BlobApplicationAware blob) {
    String blobPath = Path.of(blob.getTargetDir(), blob.getBlob() + ".decrypted").toString();

    ArrayList<BlobApplicationAware> blobSplit = new ArrayList<>();

    //Incremental integer for chunk numbering
    int chunkNum = 0;

    //Counter for current line number (from 0 to n)
    int i;

    try (
        LineIterator it = FileUtils.lineIterator(
            Path.of(blobPath).toFile(), "UTF-8")
    ) {
      while (it.hasNext()) {
        try (Writer writer = Channels.newWriter(new FileOutputStream(blobPath + "." + chunkNum,
                true).getChannel(),
            StandardCharsets.UTF_8)) {
          i = 0;
          while (i < n) {
            if (it.hasNext()) {
              String line = it.nextLine();
              writer.append(line).append("\n");
            } else {
              break;
            }
            i++;
          }
          BlobApplicationAware tmpBlob = new BlobApplicationAware(
              blob.getBlobUri() + "." + chunkNum);
          blobSplit.add(tmpBlob);
        }
        chunkNum++;
      }
    } catch (IOException e) {
      log.error("Missing blob file:{}", blobPath);
    }

    return blobSplit.stream();
  }
}
