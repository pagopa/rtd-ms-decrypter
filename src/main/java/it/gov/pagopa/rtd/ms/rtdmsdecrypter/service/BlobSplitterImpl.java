package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import static it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Status.SPLIT;
import static it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.BlobVerifierImpl.deserializeAndVerifyContract;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware.Application;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.WalletContract;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.WalletExportHeader;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Stream;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.mutable.MutableBoolean;
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
  @Value("${decrypt.splitter.aggregatesThreshold}")
  private int aggregatesLineThreshold;

  // Max number of lines allowed in one contracts blob chunk.
  @Value("${decrypt.splitter.walletThreshold}")
  private int contractsSplitThreshold;

  private String decryptedSuffix = ".decrypted";

  @Value("${decrypt.skipChecksum}")
  private boolean checksumSkipped;

  private static final String CHECKSUM_REGEX = "^#sha256.*";

  private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();

  private static final Validator validator = factory.getValidator();

  /**
   * Method that split the content of a blob in chunks of n lines.
   *
   * @param blob to be split.
   * @return a list of blobs that represent the split blob.
   */
  public Stream<BlobApplicationAware> split(BlobApplicationAware blob) {
    String blobPath = Path.of(blob.getTargetDir(), blob.getBlob() + decryptedSuffix).toString();
    ArrayList<BlobApplicationAware> blobSplit = new ArrayList<>();

    // Incremental integer for chunk numbering
    boolean successfulSplit = false;

    if (blob.getApp() == Application.ADE || blob.getApp() == Application.RTD) {
      log.info("Start splitting blob {} from {}", blob.getBlob(), blob.getContainer());
      successfulSplit = splitRtdTaeBlob(blob, blobPath, blobSplit);
    }

    if (blob.getApp() == Application.WALLET) {
      log.info("Start splitting and verifying blob {} from {}", blob.getBlob(),
          blob.getContainer());
      successfulSplit = splitWalletBlob(blob, blobPath, blobSplit);
    }

    return finalizeSplit(blob, successfulSplit, blobSplit);
  }

  private boolean splitRtdTaeBlob(BlobApplicationAware blob, String blobPath,
      ArrayList<BlobApplicationAware> blobSplit) {

    int chunkNum = 0;
    String chunkName;
    String checkSum = "";
    MutableBoolean isChecksumSkipped = new MutableBoolean(checksumSkipped);

    try (
        LineIterator it = FileUtils.lineIterator(
            Path.of(blobPath).toFile(), "UTF-8")) {
      if (it.hasNext() && isChecksumSkipped.isFalse()) {
        checkSum = it.nextLine();
        if (!checkSum.matches(CHECKSUM_REGEX)) {
          log.error("Checksum is not a conformed one {}", checkSum);
          throw new IllegalArgumentException("Error detected inside the file's checksum");
        } 
        blob.getReportMetaData().setCheckSum(checkSum);
        log.info("Checksum: {} {}", blob.getBlob(), checkSum);
        isChecksumSkipped.setTrue();
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
          writeCsvChunks(it, writer);
          BlobApplicationAware tmpBlob = new BlobApplicationAware(
              blob.getBlobUri());
          tmpBlob.setOriginalBlob(blob);
          tmpBlob.setOriginalBlobName(blob.getBlob());
          tmpBlob.setStatus(SPLIT);
          tmpBlob.setApp(blob.getApp());
          tmpBlob.setBlob(chunkName);
          tmpBlob.setBlobUri(
              blob.getBlobUri().substring(0, blob.getBlobUri().lastIndexOf("/")) + "/" + chunkName);
          tmpBlob.setNumChunk(chunkNum + 1);
          blobSplit.add(tmpBlob);
        }
        chunkNum++;
      }
      for (BlobApplicationAware blobApplicationAware : blobSplit) {
        blobApplicationAware.setTotChunk(chunkNum);
      }

    } catch (IOException e) {
      log.error("Missing blob file:{}", blobPath);
      return false;
    }
    return true;
  }

  private boolean splitWalletBlob(BlobApplicationAware blob, String blobPath,
      ArrayList<BlobApplicationAware> blobSplit) {

    ObjectMapper objectMapper = new ObjectMapper();
    JsonFactory jsonFactory = new JsonFactory();

    try (InputStream inputStream = new FileInputStream(blobPath)) {
      JsonParser jsonParser = jsonFactory.createParser(inputStream);

      if (jsonParser.nextToken() != JsonToken.START_OBJECT) {
        log.error("Validation error: malformed wallet export");
        return false;
      }

      if (jsonParser.nextToken() == JsonToken.FIELD_NAME && !jsonParser.getCurrentName()
          .equals("header")) {
        log.error("Validation error: expected wallet export header");
        return false;
      }

      jsonParser.nextToken();
      WalletExportHeader header = objectMapper.readValue(jsonParser, WalletExportHeader.class);
      Set<ConstraintViolation<WalletExportHeader>> violations = validator.validate(header);
      if (!violations.isEmpty()) {
        log.error("Validation error: malformed wallet export header");
        for (ConstraintViolation<WalletExportHeader> violation : violations) {
          log.error("{} {}", violation.getPropertyPath(), violation.getMessage());
        }
        return false;
      }

      log.info("Contracts export header:  {}", header.toString());

      if (jsonParser.getCurrentName() == null || jsonParser.nextToken() != JsonToken.FIELD_NAME
          || !jsonParser.getCurrentName().equals("contracts")) {
        log.error("Validation error: expected wallet export contracts");
        return false;
      }

      if (jsonParser.nextToken() != JsonToken.START_ARRAY) {
        log.error("Validation error: expected wallet export contracts array");
        return false;
      }

      return deserializeAndSplitContracts(jsonParser, blobSplit, objectMapper, blob);

    } catch (JsonParseException | MismatchedInputException e) {
      log.error("Validation error: malformed wallet export {}", e.getMessage());
      return false;
    } catch (IOException e) {
      log.error("Missing blob file:{}", blobPath);
      return false;
    }
  }

  private String adeNamingConvention(BlobApplicationAware blob) {
    // Note that no chunk number is added to the blob name
    return "AGGADE." + blob.getSenderCode() + "." + blob.getFileCreationDate() + "."
        + blob.getFileCreationTime() + "." + blob.getFlowNumber() + "."
        + blob.getBatchServiceChunkNumber();
  }

  private void writeCsvChunks(LineIterator it, Writer writer)
      throws IOException {
    // Counter for current line number (from 0 to aggregatesLineThreshold)
    int i = 0;
    while (it.hasNext() && i < this.aggregatesLineThreshold) {
      writer.append(it.nextLine()).append("\n");
      i++;
    }
  }

  private boolean deserializeAndSplitContracts(JsonParser jsonParser,
      ArrayList<BlobApplicationAware> blobSplit, ObjectMapper objectMapper,
      BlobApplicationAware blob)
      throws IOException {
    int contractsCounter = 0;
    int contractsSplitCounter = 0;
    int chunkNum = 0;
    boolean isChunkOpen = false;

    JsonFactory jsonFactory = new JsonFactory();
    BlobApplicationAware chunkBlob = null;
    File chunkOutputFile;
    FileWriter fileWriter = null;
    JsonGenerator jsonGenerator = null;

    // Iterate over the tokens until the end of the contracts array
    while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
      if (!isChunkOpen) {
        chunkBlob = blobChunkConstructor(blob, chunkNum);
        chunkOutputFile = new File(
            Path.of(chunkBlob.getTargetDir(), chunkBlob.getBlob()).toString());
        fileWriter = new FileWriter(chunkOutputFile);
        jsonGenerator = jsonFactory.createGenerator(fileWriter);
        jsonGenerator.writeStartArray();
        isChunkOpen = true;
      }
      try {
        WalletContract contract = deserializeAndVerifyContract(objectMapper, jsonParser,
            contractsCounter);
        if (contract == null) {
          return false;
        }
        contractsSplitCounter++;
        contractsCounter++;

        objectMapper.writeValue(jsonGenerator, contract);

        if (contractsSplitCounter % contractsSplitThreshold == 0) {
          jsonGenerator.writeEndArray();
          jsonGenerator.close();
          fileWriter.close();
          isChunkOpen = false;
          blobSplit.add(chunkBlob);
          chunkNum++;
        }
      } catch (UnrecognizedPropertyException e) {
        log.error("Failed to deserialize the contract {}: {}", contractsCounter, e.getMessage());
        return false;
      }
    }

    if (isChunkOpen) {
      jsonGenerator.writeEndArray();
      jsonGenerator.close();
      fileWriter.close();
      blobSplit.add(chunkBlob);
    }

    return true;
  }

  private Stream<BlobApplicationAware> finalizeSplit(BlobApplicationAware blob,
      boolean successfulSplit, ArrayList<BlobApplicationAware> blobSplit) {

    if (successfulSplit) {
      log.info("Obtained {} chunk/s from blob:{}", blobSplit.size(), blob.getBlob());
      for (BlobApplicationAware b : blobSplit) {
        b.setOriginalFileChunksNumber(blobSplit.size());
      }
      return blobSplit.stream();
    } else {
      // If split fails, return the original blob (without the SPLIT status)
      log.info("Failed splitting blob: {}", blob.getBlob());
      blob.localCleanup();
      return Stream.of(blob);
    }
  }

  private BlobApplicationAware blobChunkConstructor(BlobApplicationAware blob, int chunkNum) {
    String chunkName = blob.getBlob() + "." + chunkNum + decryptedSuffix;
    BlobApplicationAware chunkBlob = new BlobApplicationAware(
        blob.getBlobUri());
    chunkBlob.setOriginalBlobName(blob.getBlob());
    chunkBlob.setStatus(SPLIT);
    chunkBlob.setApp(blob.getApp());
    chunkBlob.setBlob(chunkName);
    chunkBlob.setOriginalBlob(blob);
    chunkBlob.setBlobUri(
        blob.getBlobUri().substring(0, blob.getBlobUri().lastIndexOf("/")) + "/"
            + chunkName);
    chunkBlob.setTargetDir(blob.getTargetDir());
    return chunkBlob;
  }
}
