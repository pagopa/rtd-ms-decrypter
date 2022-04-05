package it.gov.pagopa.rtd.ms.rtdmsdecrypter.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestFilesCleanup {

  public static void cleanup(Path toDelete) {
    for (File f : Objects.requireNonNull(toDelete.toFile().listFiles())) {
      try {
        Files.delete(toDelete.resolve(Path.of(f.getName())));
      } catch (IOException e) {
        log.error("Error while deleting " + f.getName() + " from "+ toDelete);
      }
    }
  }

}
