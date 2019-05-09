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

import javax.inject.Singleton;

@Singleton
public class MediaHash {

  public static void main(String[] args) {
    for(int i = 0; i < 1000000; i++) {
      if(isPowerOf(3, i) || isPowerOf(5, i) || isPowerOf(7, i)) {
        //System.out.println(i * 64.0 / 1024);
      }
    }

    int p = 0;
    int t = 64;
    int a = 64;
    for(int i = 0; i < 200; i++) {
      System.out.println(p);
      p += t;
      t += a;
      a += 64;
    }
  }

  private static boolean isPowerOf(int power, int input) {
    if(input == 0) {
      return false;
    }

    while(input % power == 0) {
      input /= power;
    }

    return input == 1;
  }

  /**
   * Computes SHA-256 hash on several blocks of 64 kB of the input file.  The blocks are chosen starting
   * with the block at offset 0 kB.  Subsequent blocks are calculated using a step size (64 kB initially),
   * an increase of the step size by step increment (64 kB initially) and step increment increased with
   * 64 kB each round.<p>
   *
   * This gives the first few blocks as:<p>
   *
   * 0, 64, 192, 448, 896, 1600, 2624, 4032, 5888, 8256, 11200, etc.
   *
   * @param uri a file to hash
   * @return a byte array containing the hash
   * @throws IOException if a read error occurs
   */
  public byte[] computeFileHash(Path path) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      try(SeekableByteChannel channel = Files.newByteChannel(path)) {
        ByteBuffer buf = ByteBuffer.allocate(65536);

        buf.putLong(channel.size());
        buf.flip();
        digest.update(buf);
        buf.clear();

        long position = 0;       // in kB
        long stepSize = 64;      // initial step size
        long stepIncrement = 64; // increment of step size (which in turn is incremented by 64 each round)

        for(;;) {
          channel.position(position * 1024);

          int bytesRead = readFully(channel, buf);
          buf.flip();
          digest.update(buf);

          if(bytesRead < buf.capacity()) {
            break;
          }

          buf.clear();

          position += stepSize;
          stepSize += stepIncrement;
          stepIncrement += 64;
        }
      }

      return digest.digest();
    }
    catch(NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Computes SHA-256 hash on all the names in the given directory.
   *
   * @param path a directory to hash
   * @return a byte array containing the hash
   * @throws IOException if a read error occurs
   */
  public byte[] computeDirectoryHash(Path path) throws IOException {
    if(!Files.isDirectory(path)) {
      throw new IllegalArgumentException("path must be a directory: " + path);
    }

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      Files.list(path).forEach(p -> digest.update(path.toUri().toString().getBytes(StandardCharsets.UTF_8)));

      return digest.digest();
    }
    catch(NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public long loadOpenSubtitlesHash(Path path) throws IOException {
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

  private static long getLongChecksum(ByteBuffer buf) {
    LongBuffer longBuffer = buf.order(ByteOrder.LITTLE_ENDIAN).asLongBuffer();
    long checksum = 0;

    while(longBuffer.hasRemaining()) {
      checksum += longBuffer.get();
    }

    return checksum;
  }

  private static int readFully(SeekableByteChannel channel, ByteBuffer buf) throws IOException {
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
