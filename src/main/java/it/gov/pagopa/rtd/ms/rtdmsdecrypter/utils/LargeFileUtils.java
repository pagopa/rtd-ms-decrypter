package it.gov.pagopa.rtd.ms.rtdmsdecrypter.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.springframework.util.Assert;

final public class LargeFileUtils {

  /**
   * A copy methods inspired by StreamUtils.copy but for a large files.
   *
   * @param in  source stream
   * @param out target stream
   * @return number of bytes copied
   * @throws IOException
   */
  public static long copy(InputStream in, OutputStream out) throws IOException {
    Assert.notNull(in, "No InputStream specified");
    Assert.notNull(out, "No OutputStream specified");
    long byteCount = 0;

    int bytesRead;
    for (byte[] buffer = new byte[4096]; (bytesRead = in.read(buffer)) != -1;
        byteCount += bytesRead) {
      out.write(buffer, 0, bytesRead);
    }

    out.flush();
    return byteCount;
  }
}
