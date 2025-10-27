package checks.security;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
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

  static class ShouldNotRaiseForSecureIvMethod {
    ByteBuffer ivBuffer;

    void should_not_raise_issue_for_secure_method_using_secure_random_next_bytes() throws InvalidAlgorithmParameterException, InvalidKeyException {
      final byte[] iv = initIvWithSecureRandom(12);
      cipher.init(ENCRYPT_MODE, secretKey, new IvParameterSpec(iv)); // Compliant
    }

    void should_not_raise_issue_for_secure_method_using_secure_random_generate() throws InvalidAlgorithmParameterException, InvalidKeyException {
      final byte[] iv = generateIvWithSecureRandom(12);
      cipher.init(ENCRYPT_MODE, secretKey, new IvParameterSpec(iv)); // Compliant
    }

    void should_not_raise_issue_for_secure_method_using_secure_random_generate_with_multiple_assignments()
      throws InvalidAlgorithmParameterException, InvalidKeyException, UnsupportedEncodingException {
      final byte[] iv = generateIvWithSecureWithMultipleAssignments(12);
      cipher.init(ENCRYPT_MODE, secretKey, new IvParameterSpec(iv)); // Compliant
    }

    void should_not_raise_issue_for_secure_method_using_byte_buffer() throws InvalidAlgorithmParameterException, InvalidKeyException {
      final byte[] iv = initIvWithByteBuffer(12);
      // Here, it is unclear whether the byte buffer used to initialize `iv` in the called method contains secure bytes or not.
      // Following the existing behaviour of this rule, we should not raise when byte buffers are involved in the initialization
      cipher.init(ENCRYPT_MODE, secretKey, new IvParameterSpec(iv)); // Unknown: Could either be a TN or FN
    }

    void should_not_raise_issue_for_secure_method_in_surrounding_class() throws InvalidAlgorithmParameterException, InvalidKeyException {
      final byte[] iv = secureIvMethodInSurroundingClass(12);
      cipher.init(ENCRYPT_MODE, secretKey, new IvParameterSpec(iv)); // Compliant
    }

    void should_not_raise_issue_for_secure_method_in_outer_class() throws InvalidAlgorithmParameterException, InvalidKeyException {
      final byte[] iv = OtherTopLevelClass.generateIvSecurely(12);
      // Accepted FP below: We not track the security of methods across separated class trees, hence, we do not know that
      // OtherTopLevelClass.generateIvSecurely is secure here
      cipher.init(ENCRYPT_MODE, secretKey, new IvParameterSpec(iv)); // Noncompliant
    }

    void should_not_raise_issue_for_method_that_calls_a_secure_method() throws InvalidAlgorithmParameterException, InvalidKeyException {
      final byte[] iv = callsAnotherSecureIvGeneratingMethod();
      // Accepted FP below: We do not track the security of IV generating methods beyond a call depth of 1.
      cipher.init(ENCRYPT_MODE, secretKey, new IvParameterSpec(iv)); // Noncompliant
    }

    void should_not_raise_issue_for_method_that_generates_iv_from_secure_param() throws InvalidAlgorithmParameterException, InvalidKeyException {
      final byte[] iv = constructIvFromParam(SECURE_RANDOM.generateSeed(12));
      // Accepted FP below: We can not trace the flow of parameters
      cipher.init(ENCRYPT_MODE, secretKey, new IvParameterSpec(iv)); // Noncompliant
    }

    private byte[] initIvWithSecureRandom(final int length) {
      final byte[] data = new byte[length];
      SECURE_RANDOM.nextBytes(data);
      return data;
    }

    private byte[] initIvWithByteBuffer(final int length) {
      final byte[] data = new byte[length];
      ivBuffer.get(data);
      return data;
    }

    private byte[] generateIvWithSecureRandom(final int length) {
      return SECURE_RANDOM.generateSeed(length);
    }

    private byte[] generateIvWithSecureWithMultipleAssignments(final int length) throws UnsupportedEncodingException {
      byte[] iv = "111".getBytes("UTF-8");
      iv = SECURE_RANDOM.generateSeed(16);

      return iv;
    }

    private byte[] callsAnotherSecureIvGeneratingMethod() {
      return callsAnotherSecureIvGeneratingMethod();
    }

    private byte[] constructIvFromParam(byte[] param) {
      return param;
    }
  }

  private static byte[] secureIvMethodInSurroundingClass(final int length) {
    return SECURE_RANDOM.generateSeed(length);
  }

  static class ShouldRaiseForInsecureIvMethod {
    void should_raise_for_insecure_iv_method() throws InvalidAlgorithmParameterException, InvalidKeyException {
      final byte[] iv = insecureIv();
      cipher.init(ENCRYPT_MODE, secretKey, new IvParameterSpec(iv)); // Noncompliant
    }

    void should_raise_for_insecure_iv_method_using_multiple_assignments() throws InvalidAlgorithmParameterException, InvalidKeyException, UnsupportedEncodingException {
      final byte[] iv = initInsecurelyWithMultipleAssignments(12);
      // FN below: If there are multiple assignments during IV generation, we do not raise if at least one of them is secure.
      // This is in line with the behaviour we have if IVs are locally generated
      cipher.init(ENCRYPT_MODE, secretKey, new IvParameterSpec(iv)); // FN
    }

    void should_raise_for_insecure_iv_method_in_surrounding_class() throws InvalidAlgorithmParameterException, InvalidKeyException {
      final byte[] iv = insecureIvMethodInSurroundingClass(12);
      cipher.init(ENCRYPT_MODE, secretKey, new IvParameterSpec(iv)); // Noncompliant
    }

    void should_raise_for_insecure_iv_method_in_outer_class() throws InvalidAlgorithmParameterException, InvalidKeyException {
      final byte[] iv = OtherTopLevelClass.generateIvInsecurely(12);
      cipher.init(ENCRYPT_MODE, secretKey, new IvParameterSpec(iv)); // Noncompliant
    }

    void should_raise_issue_for_method_that_calls_an_insecure_method() throws InvalidAlgorithmParameterException, InvalidKeyException {
      final byte[] iv = callsAnotherInsecureIvGeneratingMethod();
      cipher.init(ENCRYPT_MODE, secretKey, new IvParameterSpec(iv)); // Noncompliant
    }

    void should_raise_issue_for_method_that_generates_iv_from_insecure_param() throws InvalidAlgorithmParameterException, InvalidKeyException {
      final byte[] iv = constructIvFromParam("Insecure");
      cipher.init(ENCRYPT_MODE, secretKey, new IvParameterSpec(iv)); // Noncompliant
    }

    private byte[] insecureIv() {
      return new byte[12];
    }

    private byte[] initInsecurelyWithMultipleAssignments(final int length) throws UnsupportedEncodingException {
      byte[] iv = SECURE_RANDOM.generateSeed(16);
      iv = "111".getBytes("UTF-8");

      return iv;
    }

    private byte[] callsAnotherInsecureIvGeneratingMethod() {
      return insecureIv();
    }

    private byte[] constructIvFromParam(String param) {
      return param.getBytes();
    }
  }

  private static byte[] insecureIvMethodInSurroundingClass(final int length) {
    return new byte[length];
  }

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

class OtherTopLevelClass {
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  static byte[] generateIvSecurely(final int length) {
    return SECURE_RANDOM.generateSeed(length);
  }

  static byte[] generateIvInsecurely(final int length) {
    return new byte[length];
  }
}
