import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

class A {

  SecureRandom random = new SecureRandom();
  private byte[] bytes2 = "111".getBytes();
  {
    random.nextBytes(bytes2);
  }

  void foo1()
    throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, IllegalBlockSizeException,
    BadPaddingException {
    byte[] bytes = "111".getBytes("UTF-8");
    random.nextBytes(bytes);
    IvParameterSpec iv = new IvParameterSpec(bytes); // Compliant
  }

  void foo2() throws UnsupportedEncodingException {
    byte[] bytes = "111".getBytes("UTF-8");
    IvParameterSpec iv = new IvParameterSpec(bytes); // Noncompliant [[sc=26;ec=52]] {{Use a dynamically-generated, random IV.}}
  }

  void foo3() {
    IvParameterSpec iv = new IvParameterSpec(bytes2); // Noncompliant FP
  }

  void foo4() throws UnsupportedEncodingException {
    A a = new A();
    a.bytes2 = "111".getBytes("UTF-8");
    random.nextBytes(a.bytes2);
    IvParameterSpec iv = new IvParameterSpec(a.bytes2); // Compliant
  }

  void foo5() throws UnsupportedEncodingException {
    A a = new A();
    a.bytes2 = "111".getBytes("UTF-8");
    IvParameterSpec iv = new IvParameterSpec(a.bytes2); // Noncompliant
  }

  void foo6() throws UnsupportedEncodingException {
    IvParameterSpec iv = new IvParameterSpec("111".getBytes("UTF-8")); // Noncompliant
  }

  void foo7(byte[] bytes) {
    IvParameterSpec iv = new IvParameterSpec(bytes); // Noncompliant
  }
}
