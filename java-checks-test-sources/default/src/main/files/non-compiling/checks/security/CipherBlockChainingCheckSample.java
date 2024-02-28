package checks.security;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import javax.crypto.spec.IvParameterSpec;

class CipherBlockChainingCheckSample {

  SecureRandom random = new SecureRandom();
  private byte[] bytes2 = "111".getBytes();
  {
    random.nextBytes(bytes2);
  }
  IvParameterSpec iv = new IvParameterSpec(bytes2); // Compliant FN, will be fixed with SE engine

  static void byteBufferNotUsedAsIv(ByteBuffer ivBuffer) {
    final byte[] biv = new byte[16];
    final byte[] other = new byte[16];
    ivBuffer.get(other);
    IvParameterSpec iv = new IvParameterSpec(biv); // Noncompliant
  }

  void unknownFunctionFromRandomIv() throws UnsupportedEncodingException {
    byte[] iv = random.unknown(16);
    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv); // Compliant, because function cannot be resolved
  }

  void unknownFunctionIv() throws UnsupportedEncodingException {
    byte[] iv = unknown(16); 
    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv); // Compliant, because function cannot be resolved
  }
}
