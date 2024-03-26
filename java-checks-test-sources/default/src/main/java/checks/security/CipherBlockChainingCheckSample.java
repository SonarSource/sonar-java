package checks.security;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static checks.security.ClassWithRandomStuff.BIV;

class CipherBlockChainingCheckSample {

  SecureRandom random = new SecureRandom();
  private byte[] bytes2 = "111".getBytes();
  {
    random.nextBytes(bytes2);
  }
  IvParameterSpec iv = new IvParameterSpec(bytes2); // Compliant FN, will be fixed with SE engine

  void foo1()
    throws Exception {
    byte[] bytes = "111".getBytes("UTF-8");
    random.nextBytes(bytes);
    IvParameterSpec iv = new IvParameterSpec(bytes); // Compliant
  }

  void foo2() throws UnsupportedEncodingException {
    byte[] bytes = "111".getBytes("UTF-8");
    IvParameterSpec iv = new IvParameterSpec(bytes); // Noncompliant [[sc=26;ec=52]] {{Use a dynamically-generated, random IV.}}
  }

  void foo3() {
    IvParameterSpec iv = new IvParameterSpec(bytes2); // Noncompliant FN, will be fixed with SE engine
  }

  void foo4() throws UnsupportedEncodingException {
    CipherBlockChainingCheckSample a = new CipherBlockChainingCheckSample();
    a.bytes2 = "111".getBytes("UTF-8");
    random.nextBytes(a.bytes2);
    byte[] bytes = "111".getBytes("UTF-8");
    IvParameterSpec iv = new IvParameterSpec(a.bytes2); // Compliant
  }

  void foo5() throws UnsupportedEncodingException {
    CipherBlockChainingCheckSample a = new CipherBlockChainingCheckSample();
    a.bytes2 = "111".getBytes("UTF-8");
    IvParameterSpec iv = new IvParameterSpec(a.bytes2); // Noncompliant
  }

  void foo6() throws UnsupportedEncodingException {
    IvParameterSpec iv = new IvParameterSpec("111".getBytes("UTF-8")); // Noncompliant
  }

  void foo7(byte[] bytes) {
    IvParameterSpec iv = new IvParameterSpec(bytes); // Compliant, we can't tell what "bytes" contains
  }

  void foo8() throws UnsupportedEncodingException {
    if (true) {
      byte[] bytes = "111".getBytes("UTF-8");
      random.nextBytes(bytes);
      IvParameterSpec iv = new IvParameterSpec(bytes); // Compliant
    }
  }

  void foo9() throws UnsupportedEncodingException {
    byte[] bytes = "111".getBytes("UTF-8");
    random.nextBytes(bytes);
    if (true) {
      IvParameterSpec iv = new IvParameterSpec(bytes); // Compliant
    }
  }

  void foo10() throws UnsupportedEncodingException {
    byte[] bytes = "111".getBytes("UTF-8");
    if (true) {
      IvParameterSpec iv = new IvParameterSpec(bytes); // Noncompliant
    }
  }

  void foo11() throws UnsupportedEncodingException {
    if (true) {
      byte[] bytes = "111".getBytes("UTF-8");
      IvParameterSpec iv = new IvParameterSpec(bytes); // Noncompliant
    }
  }

  void foo12() throws UnsupportedEncodingException {
    byte[] bytes = "111".getBytes("UTF-8");
    random.nextBytes(bytes);
    IvParameterSpec iv = new IvParameterSpec(null); // Noncompliant Coverage reasons
  }

  void foo13() throws UnsupportedEncodingException {
    byte[] bytes = "111".getBytes("UTF-8");
    random.nextBytes(bytes);
    byte[] bytes2 = "111222".getBytes("UTF-8");
    IvParameterSpec iv = new IvParameterSpec(bytes2); // Noncompliant Coverage reasons
  }

  void foo14() throws UnsupportedEncodingException {
    byte[] iv = random.generateSeed(16); // "iv" is random thanks to SecureRandom
    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv); // Compliant
  }

  void foo15() throws UnsupportedEncodingException {
    IvParameterSpec ivParameterSpec = new IvParameterSpec(random.generateSeed(16)); // Compliant
  }

  void foo16() throws UnsupportedEncodingException {
    byte[] iv = random.generateSeed(16);
    iv = "111".getBytes("UTF-8");
    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv); // Compliant, FN, to avoid FP, we don't report when at least one asignment
  }

  void foo17() throws UnsupportedEncodingException {
    byte[] iv = "111".getBytes("UTF-8");
    iv = random.generateSeed(16);
    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv); // Compliant
  }

  void foo18() throws UnsupportedEncodingException {
    byte[] iv;
    iv = random.generateSeed(16);
    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv); // Compliant
  }

  void foo19() throws UnsupportedEncodingException {
    byte[] iv1 = "111".getBytes("UTF-8");
    byte[] iv2 = random.generateSeed(16);
    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv1); // Noncompliant
  }

  void foo20() throws Exception {
    new IvParameterSpec(BIV); // Noncompliant
  }

  private static final String OPERATION_MODE = "Threefish-1024/CBC/ISO10126Padding";
  private static final int IV_SIZE = 2048;

  static void encryptImpl(SecretKeySpec ks) throws Exception {
    byte[] biv = new byte[IV_SIZE];

    SecureRandom.getInstanceStrong().nextBytes(biv);

    IvParameterSpec iv = new IvParameterSpec(biv); // Compliant

    Cipher
      .getInstance(OPERATION_MODE, "BC")
      .init(Cipher.ENCRYPT_MODE, ks, iv);
  }

  static void decryptImpl1(byte[] biv, SecretKeySpec ks) throws Exception {
    IvParameterSpec iv = new IvParameterSpec(biv); // Compliant - iv is going to be use for decryption only,
                                                   // and needs to be in pair with the one used for encryption
    Cipher
      .getInstance(OPERATION_MODE, "BC")
      .init(Cipher.DECRYPT_MODE, ks, iv);
  }

  static void decryptImpl2(byte[] biv, SecretKeySpec ks) throws Exception {
    Cipher
      .getInstance(OPERATION_MODE, "BC")
      .init(Cipher.DECRYPT_MODE, ks, new IvParameterSpec(biv)); // compliant
  }

  static void decryptImpl3(SecretKeySpec ks) throws Exception {
    new IvParameterSpec("111".getBytes("UTF-8")); // Noncompliant - not used
    Cipher
      .getInstance(OPERATION_MODE, "BC")
      .init(Cipher.DECRYPT_MODE, ks);
  }

  static void decryptImpl4(SecretKeySpec ks) throws Exception {
    byte[] biv = new byte[IV_SIZE];
    IvParameterSpec iv = new IvParameterSpec(biv); // Noncompliant - not random
    Cipher
      .getInstance(OPERATION_MODE, "BC")
      .init(Cipher.ENCRYPT_MODE, ks, iv);
  }

  static void decryptImpl5(SecretKeySpec ks, IvParameterSpec iv) throws Exception {
    new IvParameterSpec("111".getBytes("UTF-8")); // Noncompliant - not used
    Cipher
      .getInstance(OPERATION_MODE, "BC")
      .init(Cipher.DECRYPT_MODE, ks, iv);
  }

  static void decryptImpl6(SecretKeySpec ks, IvParameterSpec iv) throws Exception {
    IvParameterSpec iv2 = new IvParameterSpec("111".getBytes("UTF-8")); // Noncompliant - not used
    Cipher
      .getInstance(OPERATION_MODE, "BC")
      .init(Cipher.DECRYPT_MODE, ks, iv);
  }

  static void decryptImpl17(SecretKeySpec ks) throws Exception {
    AlgorithmParameterSpec spec;
    spec = new IvParameterSpec("111".getBytes("UTF-8")); // Compliant, not random but used by Cipher#init with Cipher.DECRYPT_MODE
    Cipher
      .getInstance(OPERATION_MODE, "BC")
      .init(Cipher.DECRYPT_MODE, ks, spec);
  }

  static void decryptImpl18(byte[] biv, SecretKeySpec ks) throws Exception {
    AlgorithmParameterSpec spec;
    if (true) {
      spec = new IvParameterSpec(biv); // Compliant
    } else {
      // some other type of initialization
    }
    Cipher
      .getInstance(OPERATION_MODE, "BC")
      .init(Cipher.DECRYPT_MODE, ks, spec);
  }

  static void byteBufferUsedAsIv(ByteBuffer ivBuffer) {
    final byte[] biv1 = new byte[16];
    ivBuffer.get(biv1);
    IvParameterSpec iv1 = new IvParameterSpec(biv1); // Compliant

    final byte[] biv2 = new byte[16];
    IvParameterSpec iv2 = new IvParameterSpec(biv2); // Noncompliant

    final byte[] biv3 = new byte[16];
    ivBuffer.get(biv3, 0, 16);
    IvParameterSpec iv3 = new IvParameterSpec(biv3); // Compliant

    final byte[] biv4 = new byte[16];
    ivBuffer.get(256, biv4);
    IvParameterSpec iv4 = new IvParameterSpec(biv4); // Compliant

    final byte[] biv5 = new byte[16];
    ivBuffer.get(256, biv5, 0, 16);
    IvParameterSpec iv5 = new IvParameterSpec(biv5); // Compliant
  }

  static void byteBufferNotUsedAsIv(ByteBuffer ivBuffer) {
    final byte[] biv = new byte[16];
    final byte[] other = new byte[16];
    ivBuffer.get(other);
    IvParameterSpec iv = new IvParameterSpec(biv); // Noncompliant
  }
}

interface CipherBlockChainingCheckSampleI {
  Runnable r = () -> {
    byte[] bytes = new byte[0];
    try {
      bytes = "111".getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
    }
    IvParameterSpec iv = new IvParameterSpec(bytes);
  };
}
