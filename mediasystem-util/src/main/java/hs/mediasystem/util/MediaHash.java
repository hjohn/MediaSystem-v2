package hs.mediasystem.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MediaHash {

  public static byte[] getMediaHash(Path path) {
    try {
      return loadMediaHash(path);
    }
    catch(IOException e) {
      System.out.println("[WARN] MediaHash.getMediaHash() - exception while computing hash for '" + path + "': " + e);
      return null;
    }
  }

  /**
   * Computes SHA-256 hash on several blocks of 64 kB of the input file.  The blocks are chosen starting
   * with the block at offset 0 and then subsequently every block that is a power of 4 starting with the
   * block at offset 262144, followed by 1 MB, 4 MB, 16 MB, 64 MB, etc..
   *
   * @param uri a file to hash
   * @return a byte array containing the hash
   * @throws IOException if a read error occurs
   */
  public static byte[] loadMediaHash(Path path) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      long position = 65536;

      try(SeekableByteChannel channel = Files.newByteChannel(path)) {
        ByteBuffer buf = ByteBuffer.allocate(65536);

        buf.putLong(channel.size());
        buf.flip();
        digest.update(buf);
        buf.clear();

        for(;;) {
          int bytesRead = readFully(channel, buf);
          buf.flip();
          digest.update(buf);

          if(bytesRead < buf.capacity()) {
            break;
          }

          buf.clear();
          channel.position(position);
          position <<= 2;
        }
      }

      return digest.digest();
    }
    catch(NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public static byte[] createHash(Path path) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      digest.update(path.toUri().toString().getBytes(StandardCharsets.UTF_8));

      return digest.digest();
    }
    catch(NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public static long loadOpenSubtitlesHash(Path path) throws IOException {
    long fileLength = Files.size(path);
    long checksum = fileLength;
    int chunkSize = (int)Math.min(65536, fileLength);

    try(SeekableByteChannel channel = Files.newByteChannel(path)) {
      ByteBuffer buf = ByteBuffer.allocate(chunkSize);

      readFully(channel, buf);
      buf.flip();
      checksum += getLongChecksum(buf);
      buf.clear();

      channel.position(fileLength - chunkSize);

      readFully(channel, buf);
      buf.flip();
      checksum += getLongChecksum(buf);
    }

    return checksum;
  }

  public static long getLongChecksum(ByteBuffer buf) {
    LongBuffer longBuffer = buf.order(ByteOrder.LITTLE_ENDIAN).asLongBuffer();
    long checksum = 0;

    while(longBuffer.hasRemaining()) {
      checksum += longBuffer.get();
    }

    return checksum;
  }

  public static int readFully(SeekableByteChannel channel, ByteBuffer buf) throws IOException {
    int totalBytesRead = 0;

    while(buf.hasRemaining()) {
      int bytesRead = channel.read(buf);

      if(bytesRead == -1) {
        break;
      }
      totalBytesRead += bytesRead;
    }

    return totalBytesRead;
  }
}
