package checks.security;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static javax.crypto.Cipher.ENCRYPT_MODE;

public class CipherBlockChainingCheckShouldDetectCustomIVFactories {
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  // We use fields to omit the details of cipher and secret key creation because they are not relevant for this test.
  static Cipher cipher;
  static SecretKeySpec secretKey;

  static class Control {
    void should_raise_issue_for_insecure_iv() throws InvalidAlgorithmParameterException, InvalidKeyException {
      final byte[] insecureIv = new byte[42];
      cipher.init(ENCRYPT_MODE, secretKey, new IvParameterSpec(insecureIv)); // Noncompliant
    }
  }

  void should_not_raise_if_iv_generator_does_not_return_byte_array_01() throws InvalidAlgorithmParameterException, InvalidKeyException {
    final byte[] iv = returnsNothing();
    // This code is non-compiling and the called iv generator is not recognized as secure because it has the wrong return type.
    // We accept possible FPs in such cases:
    cipher.init(ENCRYPT_MODE, secretKey, new IvParameterSpec(iv)); // Noncompliant
  }

  void should_not_raise_if_iv_generator_does_not_return_byte_array_02() throws InvalidAlgorithmParameterException, InvalidKeyException {
    final byte[] iv = returnsIntArray(42);
    // This code is non-compiling and the called iv generator is not recognized as secure because it has the wrong return type.
    // We accept possible FPs in such cases:
    cipher.init(ENCRYPT_MODE, secretKey, new IvParameterSpec(iv)); // Noncompliant
  }

  private void returnsNothing() {
  }

  private int[] returnsIntArray(final int length) {
    return new int[length];
  }
}
