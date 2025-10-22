package checks.security;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import static javax.crypto.Cipher.ENCRYPT_MODE;

public class CipherBlockChainingCheckShouldDetectCustomIVFactories {
  // This is a reproducer for a former FP
  static class SONARJAVA4895Reproducer {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public void calls_method_to_generate_iv()
      throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, InvalidKeySpecException {

      final var spec = new PBEKeySpec("Key".toCharArray(), generateRandomData(16), 65536, 256);
      final var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
      final var secretKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");

      final var cipher = Cipher.getInstance("ChaCha20-Poly1305");

      final byte[] iv = generateRandomData(12);
      // An FP used to be raised here because iv is securely generated in a separate method, and we were not able to trace that.
      cipher.init(ENCRYPT_MODE, secretKey, new IvParameterSpec(iv)); // Compliant
    }

    private byte[] generateRandomData(final int length) {
      final byte[] data = new byte[length];
      SECURE_RANDOM.nextBytes(data);
      return data;
    }
  }
}
