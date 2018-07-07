package hs.mediasystem.util;

public class ByteArrays {
  private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

  public static String toHex(byte[] data) {
    StringBuilder builder = new StringBuilder();
    int length = data.length;

    for(int i=0; i < length; i++) {
      builder
        .append(HEX_DIGITS[(0xf0 & data[i]) >>> 4])
        .append(HEX_DIGITS[0x0f & data[i]]);
    }

    return builder.toString();
  }
}
