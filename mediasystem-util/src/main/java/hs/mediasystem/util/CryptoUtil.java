package hs.mediasystem.util;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtil {

  public static String encrypt(String plainText, String key) {
    try {
      return toHex(encrypt(getRawKey(key.getBytes()), plainText.getBytes()));
    }
    catch(Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static String decrypt(String hexString, String key) {
    try {
      return new String(decrypt(getRawKey(key.getBytes()), toByte(hexString)));
    }
    catch(Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static byte[] getRawKey(byte[] key) throws NoSuchAlgorithmException {
    KeyGenerator kgen = KeyGenerator.getInstance("AES");
    SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");

    sr.setSeed(key);
    kgen.init(128, sr);

    return kgen.generateKey().getEncoded();
  }

  private static byte[] encrypt(byte[] raw, byte[] clear) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
    Cipher cipher = Cipher.getInstance("AES");

    cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(raw, "AES"));

    return cipher.doFinal(clear);
  }

  private static byte[] decrypt(byte[] raw, byte[] encrypted) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
    Cipher cipher = Cipher.getInstance("AES");

    cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(raw, "AES"));

    return cipher.doFinal(encrypted);
  }

  public static byte[] toByte(String hexString) {
    int len = hexString.length() / 2;
    byte[] result = new byte[len];

    for(int i = 0; i < len; i++) {
      result[i] = (byte)Integer.parseInt(hexString.substring(2 * i, 2 * i + 2), 16);
    }

    return result;
  }

  public static String toHex(byte[] buf) {
    if(buf == null) {
      return "";
    }

    StringBuilder result = new StringBuilder(2 * buf.length);

    for(int i = 0; i < buf.length; i++) {
      result.append(HEX.charAt((buf[i] >> 4) & 0x0f)).append(HEX.charAt(buf[i] & 0x0f));
    }

    return result.toString();
  }

  private static final String HEX = "0123456789ABCDEF";
}
