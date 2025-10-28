package checks.security;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static javax.crypto.Cipher.ENCRYPT_MODE;

// This file contains some trivial cases to check that CipherBlockChainingCheck resets its class/file-level state properly.
// For this purpose, the tests of CipherBlockChainingCheck analyze this file immediately after and before the main test sources.
//
// See the other test sources for proper tests of CipherBlockChainingCheck.
public class CipherBlockChainingCheckShouldResetState {
  static Cipher cipher;
  static SecretKeySpec secretKey;

  void tn_control() throws InvalidAlgorithmParameterException, InvalidKeyException {
    final byte[] iv = secureIv();
    cipher.init(ENCRYPT_MODE, secretKey, new IvParameterSpec(iv)); // Compliant
  }

  void tp_control() throws InvalidAlgorithmParameterException, InvalidKeyException {
    final byte[] iv = insecureIv();
    cipher.init(ENCRYPT_MODE, secretKey, new IvParameterSpec(iv)); // Noncompliant
  }

  private byte[] secureIv() {
    return new SecureRandom().generateSeed(12);
  }

  private byte[] insecureIv() {
    return new byte[12];
  }
}
